#version 330

/*
 * Mahikari lightmap shader.
 *
 * Identical to vanilla, plus a fullbright sentinel:
 * LightmapTextureManagerMixin writes 100 + fullBrightLevel into NightVisionFactor
 * when the toggle is on (vanilla uses [0,1]). Any value > 50 here is reserved as
 * the marker; we strip it from the normal night-vision branch and apply a final
 * blend toward white by (NightVisionFactor - 100).
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
    float maxInverted = 1.0f - maxComponent;
    float maxScaled = 1.0f - maxInverted * maxInverted * maxInverted * maxInverted;
    return color * (maxScaled / maxComponent);
}

void main() {
    bool fullbright = lightmapInfo.NightVisionFactor > 50.0;
    float effectiveNightVision = fullbright ? 0.0 : lightmapInfo.NightVisionFactor;

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

    if (lightmapInfo.AmbientLightFactor == 0.0f) {
        vec3 darkened_color = color * vec3(0.7, 0.6, 0.6);
        color = mix(color, darkened_color, lightmapInfo.DarkenWorldFactor);
    }

    if (effectiveNightVision > 0.0) {
        float max_component = max(color.r, max(color.g, color.b));
        if (max_component < 1.0) {
            vec3 bright_color = color / max_component;
            color = mix(color, bright_color, effectiveNightVision);
        }
    }

    if (lightmapInfo.AmbientLightFactor == 0.0f) {
        color = color - vec3(lightmapInfo.DarknessScale);
    }

    color = clamp(color, 0.0, 1.0);

    vec3 notGammaColor = notGamma(color);
    color = mix(color, notGammaColor, lightmapInfo.BrightnessFactor);
    color = mix(color, vec3(0.75), 0.04);

    if (fullbright) {
        float strength = clamp(lightmapInfo.NightVisionFactor - 100.0, 0.0, 1.0);
        float ambient = 0.45 + strength * 0.55;
        color.r = max(color.r, ambient);
        color.g = max(color.g, ambient);
        color.b = max(color.b, ambient);
        color = clamp(color, 0.0, 1.0);
    }

    fragColor = vec4(color, 1.0);
}
