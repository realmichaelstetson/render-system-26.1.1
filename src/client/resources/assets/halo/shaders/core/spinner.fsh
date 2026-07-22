#version 330

in vec4 vertexColor;
in vec4 vertexColor2;
in vec2 texCoord;
in vec2 rectSize;
in float radius;
in float borderThickness;
in vec4 shadowProps;

out vec4 fragColor;

float sdArc(in vec2 p, in float startAngle, in float sweepAngle, in float r, in float t) {
    float angle = atan(p.y, p.x);
    float relAngle = mod(angle - startAngle, 2.0 * 3.14159265);
    if (relAngle < 0.0) relAngle += 2.0 * 3.14159265;
    
    vec2 startPt = r * vec2(cos(startAngle), sin(startAngle));
    vec2 endPt = r * vec2(cos(startAngle + sweepAngle), sin(startAngle + sweepAngle));
    
    if (relAngle > sweepAngle) {
        return min(length(p - startPt), length(p - endPt)) - t * 0.5;
    }
    
    return abs(length(p) - r) - t * 0.5;
}

void main() {
    // texCoord is in [0, 1]
    vec2 p = (texCoord - 0.5) * rectSize;
    
    float startAngle = shadowProps.y;
    float sweepAngle = shadowProps.z;
    float r = radius;
    float t = borderThickness;
    
    float dist = sdArc(p, startAngle, sweepAngle, r, t);
    float aa = fwidth(dist);
    
    float alpha = 1.0 - smoothstep(-aa * 0.5, aa * 0.5, dist);
    
    vec4 finalColor = vertexColor;
    finalColor.rgb *= finalColor.a;
    finalColor.a *= alpha;
    
    if (finalColor.a <= 0.001) discard;
    
    fragColor = finalColor;
}
