#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    // Ignorujemy teksturę gracza, rysujemy pełny, solidny kolor sylwetki
    // To gwarantuje, że środek postaći nie będzie pusty w entityOutlineTarget
    fragColor = vec4(ColorModulator.rgb * vertexColor.rgb, 1.0);
}
