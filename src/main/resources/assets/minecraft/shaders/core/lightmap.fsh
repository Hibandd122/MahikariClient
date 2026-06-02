#version 330

/*
    Mahikari lightmap shader (vanilla + runtime fullbright).

    Vanilla path runs unchanged when NightVisionFactor is in its normal [0,1]
    range. When LightmapTextureManagerMixin wants fullbright on, it writes
    NightVisionFactor = 100.0 + fullBrightLevel into the UBO. The shader
    detects the marker (> 100) and blends the lightmap toward white by
    `fullBrightLevel`. Darkness status effect is ALWAYS subtracted (no hack).
*/

layout(std140) uniform LightmapInfo {
    float AmbientLightFactor;
    float SkyFactor;
    float BlockFactor;
    float NightVisionFactor;
    float DarknessScale;
    float DarkenWorldFactor;
    float BrightnessFactor;
    vec3 SkyLightColor;
    vec3 AmbientColor;
} lightmapInfo;

in vec2 texCoord;
out vec4 fragColor;

float get_brightness(float level) {
    return level / (4.0 - 3.0 * level);
}

vec3 notGamma(vec3 color) {
    float maxComponent = max(max(color.x, color.y), color.z);
    float maxInverted = 1.0 - maxComponent;
    float maxScaled = 1.0 - maxInverted * maxInverted * maxInverted * maxInverted;
    return color * (maxScaled / maxComponent);
}

void main() {
    // Detect fullbright marker. Vanilla NightVisionFactor is clamped to [0,1].
    bool fullbright = lightmapInfo.NightVisionFactor > 50.0;
    float fbLevel = fullbright ? clamp(lightmapInfo.NightVisionFactor - 100.0, 0.0, 1.0) : 0.0;
    float nightVision = fullbright ? 0.0 : lightmapInfo.NightVisionFactor;

    float block_brightness = get_brightness(floor(texCoord.x * 16) / 15) * lightmapInfo.BlockFactor;
    float sky_brightness = get_brightness(floor(texCoord.y * 16) / 15) * lightmapInfo.SkyFactor;

    vec3 color = vec3(
        block_brightness,
        block_brightness * ((block_brightness * 0.6 + 0.4) * 0.6 + 0.4),
        block_brightness * (block_brightness * block_brightness * 0.6 + 0.4)
    );

    color = mix(color, lightmapInfo.AmbientColor, lightmapInfo.AmbientLightFactor);
    color += lightmapInfo.SkyLightColor * sky_brightness;
    color = mix(color, vec3(0.75), 0.04);

    if (lightmapInfo.AmbientLightFactor == 0.0) {
        vec3 darkened_color = color * vec3(0.7, 0.6, 0.6);
        color = mix(color, darkened_color, lightmapInfo.DarkenWorldFactor);
    }

    if (nightVision > 0.0) {
        float max_component = max(color.r, max(color.g, color.b));
        if (max_component < 1.0) {
            vec3 bright_color = color / max_component;
            color = mix(color, bright_color, nightVision);
        }
    }

    // Fullbright blend toward white, scaled by user-controlled level.
    if (fullbright) {
        color = mix(color, vec3(1.0), fbLevel);
    }

    // Darkness status effect — applied in BOTH modes so the player can still be blinded.
    if (lightmapInfo.AmbientLightFactor == 0.0) {
        color = color - vec3(lightmapInfo.DarknessScale);
    }

    color = clamp(color, 0.0, 1.0);

    vec3 ng = notGamma(color);
    color = mix(color, ng, lightmapInfo.BrightnessFactor);
    color = mix(color, vec3(0.75), 0.04);

    fragColor = vec4(color, 1.0);
}
