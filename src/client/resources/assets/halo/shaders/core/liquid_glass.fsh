#version 150

#moj_import <halo:common.glsl>

in vec2 FragCoord;
in vec2 TexCoord;
in vec4 FragColor;
in vec4 vCustomData;
in vec4 vColor2;
in vec4 vShadowProps;

uniform sampler2D Sampler0;

out vec4 OutColor;

void main() {
    vec2 size = vCustomData.xy;
    float rectRadius = vCustomData.z;
    float bloom = vShadowProps.y;
    float cornerMask = vShadowProps.w;
    float blurStrength = vShadowProps.x;

    // Center of coordinate system for card
    vec2 expandedSize = size + vec2(bloom * 2.0);
    vec2 p = (FragCoord - 0.5) * expandedSize;
    vec2 b = size * 0.5;
    vec2 normP = p / b;

    // SDF calculation
    float r = rectRadius;
    if (cornerMask == 1.0 && p.x > 0.0) r = 0.0;
    else if (cornerMask == 2.0 && p.x < 0.0) r = 0.0;
    else if (cornerMask == 3.0 && p.y < 0.0) r = 0.0;
    else if (cornerMask == 4.0 && p.y > 0.0) r = 0.0;

    vec2 q = abs(p) - b + r;
    float dist = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;

    float aa = fwidth(dist);
    float mainAlpha = 1.0 - smoothstep(-aa * 0.5, aa * 0.5, dist);

    // 1. Refraction: Warp TexCoord (screen UV) inside the card
    vec2 screenUV = TexCoord;
    if (mainAlpha > 0.01 && r > 0.1) {
        // Find direction of the bevel/curve pointing outwards
        vec2 curveDir = sign(p) * normalize(max(q, vec2(0.0)) + vec2(0.0001));
        // Bevel factor transitions from 0.0 (inside bevel limit) to 1.0 (at the exact edge)
        float edgeDistFactor = clamp((dist + r) / r, 0.0, 1.0);
        // Warp inward to simulate curved glass lens contraction at the borders
        float warpAmt = pow(edgeDistFactor, 3.0) * 0.018 * mainAlpha;
        screenUV -= curveDir * warpAmt;
    }

    // 2. Gaussian Blur using the refracted UV
    vec2 texSize = textureSize(Sampler0, 0);
    vec3 average = vec3(0.0);

    if (blurStrength > 0.05) {
        float radius = min(blurStrength, 30.0);
        float sigma = radius / 2.0; 
        float sigma2 = 2.0 * sigma * sigma;
        float totalWeight = 0.0;
        float stepAmount = max(1.0, radius / 12.0); 

        for (float x = -radius; x <= radius; x += stepAmount) {
            for (float y = -radius; y <= radius; y += stepAmount) {
                float weight = exp(-(x * x + y * y) / sigma2);
                vec2 offset = vec2(x, y) / texSize;
                average += texture(Sampler0, screenUV + offset).rgb * weight;
                totalWeight += weight;
            }
        }
        average /= totalWeight;
    } else {
        average = texture(Sampler0, screenUV).rgb;
    }

    // Gradient Tint Logic
    float gradientAngle = vShadowProps.z;
    float ga = radians(gradientAngle);
    vec2 gDir = vec2(sin(ga), cos(ga));
    float gt = dot(FragCoord - 0.5, gDir) + 0.5;
    gt = smoothstep(0.0, 1.0, clamp(gt, 0.0, 1.0));
    
    vec4 col1 = FragColor;
    vec4 col2 = vColor2;
    col1.rgb *= col1.a;
    col2.rgb *= col2.a;
    
    vec4 mixedTint = mix(col1, col2, gt);
    if (mixedTint.a > 0.001) mixedTint.rgb /= mixedTint.a;

    // Combine blurred background with custom tint
    vec3 baseColor = mix(average, mixedTint.rgb, mixedTint.a);

    // Apply base blurred color directly
    vec3 finalRGB = baseColor;

    vec4 finalColor = vec4(finalRGB, 1.0);

    // Bloom (Outer Glow) disabled for liquid glass
    finalColor.a = mainAlpha;

    if (finalColor.a <= 0.001) discard;

    OutColor = finalColor;
}
