#version 330

in vec4 vertexColor;
in vec4 vertexColor2;
in vec2 texCoord;
in vec2 rectSize;
in float radius; // thickness of line (half-width)
in float borderThickness; // progress (0.0 to 1.0)
in vec4 shadowProps;

out vec4 fragColor;

float sdSegment(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a, ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h);
}

void main() {
    // Center coordinates
    vec2 p = (texCoord - vec2(0.5)) * rectSize;

    float progress = borderThickness;
    
    // We want the chevron to fit nicely inside the rectSize box
    float halfW = rectSize.x * 0.35;
    float halfH = rectSize.y * 0.18;
    
    // Progress 0: Tip points down (Tip is lower on screen/positive Y, arms are higher/negative Y)
    // Progress 1: Tip points up (Tip is higher on screen/negative Y, arms are lower/positive Y)
    float tipY = mix(halfH, -halfH, progress);
    float armY = mix(-halfH, halfH, progress);
    
    vec2 tip = vec2(0.0, tipY);
    vec2 left = vec2(-halfW, armY);
    vec2 right = vec2(halfW, armY);
    
    float dLeft = sdSegment(p, tip, left);
    float dRight = sdSegment(p, tip, right);
    float dist = min(dLeft, dRight) - radius;
    
    float aa = fwidth(dist);
    float alpha = 1.0 - smoothstep(-aa * 0.5, aa * 0.5, dist);
    
    vec4 finalColor = vertexColor;
    finalColor.a *= alpha;
    
    if (finalColor.a <= 0.001) discard;
    fragColor = finalColor;
}
