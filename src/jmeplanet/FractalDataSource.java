/*
 Copyright (c) 2012 Aaron Perkins

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */
package jmeplanet;

import com.jme3.math.Vector3f;

/**
 * FractalDataSource generates height values from a 3D fractal noise source.
 *
 * Credits This code has been adapted from noise++ Copyright (c) 2008, Urs C.
 * Hanselmann http://sourceforge.net/projects/noisepp/
 */
public class FractalDataSource implements HeightDataSource {

    protected static final int NOISE_QUALITY_LOW = 0;
    protected static final int NOISE_QUALITY_STD = 1;
    protected static final int NOISE_QUALITY_HIGH = 2;
    protected static final int NOISE_QUALITY_FAST_LOW = 3;
    protected static final int NOISE_QUALITY_FAST_STD = 4;
    protected static final int NOISE_QUALITY_FAST_HIGH = 5;
    protected static final int NOISE_X_FACTOR = 1619;
    protected static final int NOISE_Y_FACTOR = 31337;
    protected static final int NOISE_Z_FACTOR = 6971;
    protected static final int NOISE_SEED_FACTOR = 1013;
    protected static final int NOISE_SHIFT = 8;
    protected static final float FAST_NOISE_SCALE_FACTOR = 0.5f;

    private class Octave {

        int seed;
        float scale;
        float persistence;
    }

    /// The noise frequency.
    private float frequency = 1.0f;
    /// The number of octaves.
    private int octaveCount = 12;
    /// The noise seed.
    private int seed = 0;
    /// The noise quality.
    private int quality = NOISE_QUALITY_STD;
    /// The noise lacunarity.
    private float lacunarity = 2.0f;
    /// The noise persistence.
    private float persistence = 0.625f;
    /// The noise scale factor.
    private float scale = 2.12f;

    private Octave[] octaves;

    private float heightScale = 1f;
    private float shift = 1f;
    private boolean minEnabled = false;
    private float min = 0f;
    private float max = 1.5f;

    public FractalDataSource() {
        this(0);
    }

    public FractalDataSource(int seed) {

        this.seed = seed;

        if (this.quality > NOISE_QUALITY_HIGH) {
            this.scale *= FAST_NOISE_SCALE_FACTOR;
        }

        this.octaves = new Octave[this.octaveCount];
        float curPersistence = 1.0f;
        int oSeed;
        float oScale = this.frequency;
        for (int o = 0; o < this.octaveCount; ++o) {
            oSeed = (this.seed + o) & 0xffffffff;
            this.octaves[o] = new Octave();
            this.octaves[o].persistence = curPersistence;
            this.octaves[o].scale = oScale;
            this.octaves[o].seed = oSeed;

            oScale *= lacunarity;
            curPersistence *= persistence;
        }

    }

    public void setMin(float min) {
        this.min = min;
        this.minEnabled = true;
    }

    public void setHeightScale(float heightScale) {
        this.heightScale = heightScale;
    }

    public int getSeed() {
        return this.seed;
    }

    public float getHeightScale() {
        return this.heightScale;
    }

    public float getValue(Vector3f position) {
        float value = 0.0f;
        float signal = 1.0f;

        for (int o = 0; o < this.octaveCount; ++o) {
            float nx = (position.x * this.octaves[o].scale);
            float ny = (position.y * this.octaves[o].scale);
            float nz = (position.z * this.octaves[o].scale);
            signal = calculateGradient(nx, ny, nz, this.octaves[o].seed);

            value += signal * this.octaves[o].persistence;
        }

        // return value, shifting, clamping, and scaling
        //return FastMath.clamp((value + shift) / (shift * 2), min, max) * heightScale;
        value *= heightScale;

        if (this.minEnabled) {
            value = Math.max(value, this.min);
        }

        return value;
    }

    private float calculateGradient(float x, float y, float z, int seed) {
        //if (this.quality == NOISE_QUALITY_STD)
        return calcGradientCoherentNoiseStd(x, y, z, seed, this.scale);
        /*
         else if (this.quality == NOISE_QUALITY_HIGH)
         return Generator3D::calcGradientCoherentNoiseHigh (x, y, z, seed, mScale);
         else if (this.quality == NOISE_QUALITY_LOW)
         return Generator3D::calcGradientCoherentNoiseLow (x, y, z, seed, mScale);
         else if (this.quality == NOISE_QUALITY_FAST_STD)
         return Generator3D::calcGradientCoherentFastNoiseStd (x, y, z, seed, mScale);
         else if (this.quality == NOISE_QUALITY_FAST_HIGH)
         return Generator3D::calcGradientCoherentFastNoiseHigh (x, y, z, seed, mScale);
         else
         return Generator3D::calcGradientCoherentFastNoiseLow (x, y, z, seed, mScale);
         * 
         */
    }

    private float calcGradientCoherentNoiseStd(float x, float y, float z, int seed, float scale) {
        //NOISE_GENERATOR_INTEGER_CLAMP_3D;
        int x0 = (x > 0.0f ? (int) x : (int) x - 1);
        int x1 = x0 + 1;
        int y0 = (y > 0.0f ? (int) y : (int) y - 1);
        int y1 = y0 + 1;
        int z0 = (z > 0.0f ? (int) z : (int) z - 1);
        int z1 = z0 + 1;

        float xs = cubicCurve3(x - x0);
        float ys = cubicCurve3(y - y0);
        float zs = cubicCurve3(z - z0);

        return interpGradientCoherentNoise(x, y, z, x0, x1, y0, y1, z0, z1, xs, ys, zs, seed, scale);
    }

    private float interpGradientCoherentNoise(float x, float y, float z, int x0, int x1, int y0, int y1, int z0, int z1, float xs, float ys, float zs, int seed, float scale) {
        float n0, n1, ix0, ix1, iy0, iy1;
        n0 = calcGradientNoise(x, y, z, x0, y0, z0, seed);
        n1 = calcGradientNoise(x, y, z, x1, y0, z0, seed);
        ix0 = interpLinear(n0, n1, xs);
        n0 = calcGradientNoise(x, y, z, x0, y1, z0, seed);
        n1 = calcGradientNoise(x, y, z, x1, y1, z0, seed);
        ix1 = interpLinear(n0, n1, xs);
        iy0 = interpLinear(ix0, ix1, ys);
        n0 = calcGradientNoise(x, y, z, x0, y0, z1, seed);
        n1 = calcGradientNoise(x, y, z, x1, y0, z1, seed);
        ix0 = interpLinear(n0, n1, xs);
        n0 = calcGradientNoise(x, y, z, x0, y1, z1, seed);
        n1 = calcGradientNoise(x, y, z, x1, y1, z1, seed);
        ix1 = interpLinear(n0, n1, xs);
        iy1 = interpLinear(ix0, ix1, ys);

        return interpLinear(iy0, iy1, zs) * scale;
    }

    private float calcGradientNoise(float fx, float fy, float fz, int ix, int iy, int iz, int seed) {
        int vIndex = (NOISE_X_FACTOR * ix + NOISE_Y_FACTOR * iy + NOISE_Z_FACTOR * iz + NOISE_SEED_FACTOR * seed) & 0xffffffff;
        vIndex ^= (vIndex >> NOISE_SHIFT);
        vIndex &= 0xff;

        float xGradient = randomVectors3D[(vIndex << 2)];
        float yGradient = randomVectors3D[(vIndex << 2) + 1];
        float zGradient = randomVectors3D[(vIndex << 2) + 2];

        float xDelta = fx - ix;
        float yDelta = fy - iy;
        float zDelta = fz - iz;
        return (xGradient * xDelta + yGradient * yDelta + zGradient * zDelta);
    }

    /// Calculates a third-order interpolant
    private float cubicCurve3(float a) {
        return (a * a * (3f - 2f * a));
    }

    /// Performs linear interpolation
    private float interpLinear(float left, float right, float a) {
        return ((1f - a) * left) + (a * right);
    }

    private final static float[] randomVectors3D = new float[]{
        -0.763874f, -0.596439f, -0.246489f, 0.0f,
        0.396055f, 0.904518f, -0.158073f, 0.0f,
        -0.499004f, -0.8665f, -0.0131631f, 0.0f,
        0.468724f, -0.824756f, 0.316346f, 0.0f,
        0.829598f, 0.43195f, 0.353816f, 0.0f,
        -0.454473f, 0.629497f, -0.630228f, 0.0f,
        -0.162349f, -0.869962f, -0.465628f, 0.0f,
        0.932805f, 0.253451f, 0.256198f, 0.0f,
        -0.345419f, 0.927299f, -0.144227f, 0.0f,
        -0.715026f, -0.293698f, -0.634413f, 0.0f,
        -0.245997f, 0.717467f, -0.651711f, 0.0f,
        -0.967409f, -0.250435f, -0.037451f, 0.0f,
        0.901729f, 0.397108f, -0.170852f, 0.0f,
        0.892657f, -0.0720622f, -0.444938f, 0.0f,
        0.0260084f, -0.0361701f, 0.999007f, 0.0f,
        0.949107f, -0.19486f, 0.247439f, 0.0f,
        0.471803f, -0.807064f, -0.355036f, 0.0f,
        0.879737f, 0.141845f, 0.453809f, 0.0f,
        0.570747f, 0.696415f, 0.435033f, 0.0f,
        -0.141751f, -0.988233f, -0.0574584f, 0.0f,
        -0.58219f, -0.0303005f, 0.812488f, 0.0f,
        -0.60922f, 0.239482f, -0.755975f, 0.0f,
        0.299394f, -0.197066f, -0.933557f, 0.0f,
        -0.851615f, -0.220702f, -0.47544f, 0.0f,
        0.848886f, 0.341829f, -0.403169f, 0.0f,
        -0.156129f, -0.687241f, 0.709453f, 0.0f,
        -0.665651f, 0.626724f, 0.405124f, 0.0f,
        0.595914f, -0.674582f, 0.43569f, 0.0f,
        0.171025f, -0.509292f, 0.843428f, 0.0f,
        0.78605f, 0.536414f, -0.307222f, 0.0f,
        0.18905f, -0.791613f, 0.581042f, 0.0f,
        -0.294916f, 0.844994f, 0.446105f, 0.0f,
        0.342031f, -0.58736f, -0.7335f, 0.0f,
        0.57155f, 0.7869f, 0.232635f, 0.0f,
        0.885026f, -0.408223f, 0.223791f, 0.0f,
        -0.789518f, 0.571645f, 0.223347f, 0.0f,
        0.774571f, 0.31566f, 0.548087f, 0.0f,
        -0.79695f, -0.0433603f, -0.602487f, 0.0f,
        -0.142425f, -0.473249f, -0.869339f, 0.0f,
        -0.0698838f, 0.170442f, 0.982886f, 0.0f,
        0.687815f, -0.484748f, 0.540306f, 0.0f,
        0.543703f, -0.534446f, -0.647112f, 0.0f,
        0.97186f, 0.184391f, -0.146588f, 0.0f,
        0.707084f, 0.485713f, -0.513921f, 0.0f,
        0.942302f, 0.331945f, 0.043348f, 0.0f,
        0.499084f, 0.599922f, 0.625307f, 0.0f,
        -0.289203f, 0.211107f, 0.9337f, 0.0f,
        0.412433f, -0.71667f, -0.56239f, 0.0f,
        0.87721f, -0.082816f, 0.47291f, 0.0f,
        -0.420685f, -0.214278f, 0.881538f, 0.0f,
        0.752558f, -0.0391579f, 0.657361f, 0.0f,
        0.0765725f, -0.996789f, 0.0234082f, 0.0f,
        -0.544312f, -0.309435f, -0.779727f, 0.0f,
        -0.455358f, -0.415572f, 0.787368f, 0.0f,
        -0.874586f, 0.483746f, 0.0330131f, 0.0f,
        0.245172f, -0.0838623f, 0.965846f, 0.0f,
        0.382293f, -0.432813f, 0.81641f, 0.0f,
        -0.287735f, -0.905514f, 0.311853f, 0.0f,
        -0.667704f, 0.704955f, -0.239186f, 0.0f,
        0.717885f, -0.464002f, -0.518983f, 0.0f,
        0.976342f, -0.214895f, 0.0240053f, 0.0f,
        -0.0733096f, -0.921136f, 0.382276f, 0.0f,
        -0.986284f, 0.151224f, -0.0661379f, 0.0f,
        -0.899319f, -0.429671f, 0.0812908f, 0.0f,
        0.652102f, -0.724625f, 0.222893f, 0.0f,
        0.203761f, 0.458023f, -0.865272f, 0.0f,
        -0.030396f, 0.698724f, -0.714745f, 0.0f,
        -0.460232f, 0.839138f, 0.289887f, 0.0f,
        -0.0898602f, 0.837894f, 0.538386f, 0.0f,
        -0.731595f, 0.0793784f, 0.677102f, 0.0f,
        -0.447236f, -0.788397f, 0.422386f, 0.0f,
        0.186481f, 0.645855f, -0.740335f, 0.0f,
        -0.259006f, 0.935463f, 0.240467f, 0.0f,
        0.445839f, 0.819655f, -0.359712f, 0.0f,
        0.349962f, 0.755022f, -0.554499f, 0.0f,
        -0.997078f, -0.0359577f, 0.0673977f, 0.0f,
        -0.431163f, -0.147516f, -0.890133f, 0.0f,
        0.299648f, -0.63914f, 0.708316f, 0.0f,
        0.397043f, 0.566526f, -0.722084f, 0.0f,
        -0.502489f, 0.438308f, -0.745246f, 0.0f,
        0.0687235f, 0.354097f, 0.93268f, 0.0f,
        -0.0476651f, -0.462597f, 0.885286f, 0.0f,
        -0.221934f, 0.900739f, -0.373383f, 0.0f,
        -0.956107f, -0.225676f, 0.186893f, 0.0f,
        -0.187627f, 0.391487f, -0.900852f, 0.0f,
        -0.224209f, -0.315405f, 0.92209f, 0.0f,
        -0.730807f, -0.537068f, 0.421283f, 0.0f,
        -0.0353135f, -0.816748f, 0.575913f, 0.0f,
        -0.941391f, 0.176991f, -0.287153f, 0.0f,
        -0.154174f, 0.390458f, 0.90762f, 0.0f,
        -0.283847f, 0.533842f, 0.796519f, 0.0f,
        -0.482737f, -0.850448f, 0.209052f, 0.0f,
        -0.649175f, 0.477748f, 0.591886f, 0.0f,
        0.885373f, -0.405387f, -0.227543f, 0.0f,
        -0.147261f, 0.181623f, -0.972279f, 0.0f,
        0.0959236f, -0.115847f, -0.988624f, 0.0f,
        -0.89724f, -0.191348f, 0.397928f, 0.0f,
        0.903553f, -0.428461f, -0.00350461f, 0.0f,
        0.849072f, -0.295807f, -0.437693f, 0.0f,
        0.65551f, 0.741754f, -0.141804f, 0.0f,
        0.61598f, -0.178669f, 0.767232f, 0.0f,
        0.0112967f, 0.932256f, -0.361623f, 0.0f,
        -0.793031f, 0.258012f, 0.551845f, 0.0f,
        0.421933f, 0.454311f, 0.784585f, 0.0f,
        -0.319993f, 0.0401618f, -0.946568f, 0.0f,
        -0.81571f, 0.551307f, -0.175151f, 0.0f,
        -0.377644f, 0.00322313f, 0.925945f, 0.0f,
        0.129759f, -0.666581f, -0.734052f, 0.0f,
        0.601901f, -0.654237f, -0.457919f, 0.0f,
        -0.927463f, -0.0343576f, -0.372334f, 0.0f,
        -0.438663f, -0.868301f, -0.231578f, 0.0f,
        -0.648845f, -0.749138f, -0.133387f, 0.0f,
        0.507393f, -0.588294f, 0.629653f, 0.0f,
        0.726958f, 0.623665f, 0.287358f, 0.0f,
        0.411159f, 0.367614f, -0.834151f, 0.0f,
        0.806333f, 0.585117f, -0.0864016f, 0.0f,
        0.263935f, -0.880876f, 0.392932f, 0.0f,
        0.421546f, -0.201336f, 0.884174f, 0.0f,
        -0.683198f, -0.569557f, -0.456996f, 0.0f,
        -0.117116f, -0.0406654f, -0.992285f, 0.0f,
        -0.643679f, -0.109196f, -0.757465f, 0.0f,
        -0.561559f, -0.62989f, 0.536554f, 0.0f,
        0.0628422f, 0.104677f, -0.992519f, 0.0f,
        0.480759f, -0.2867f, -0.828658f, 0.0f,
        -0.228559f, -0.228965f, -0.946222f, 0.0f,
        -0.10194f, -0.65706f, -0.746914f, 0.0f,
        0.0689193f, -0.678236f, 0.731605f, 0.0f,
        0.401019f, -0.754026f, 0.52022f, 0.0f,
        -0.742141f, 0.547083f, -0.387203f, 0.0f,
        -0.00210603f, -0.796417f, -0.604745f, 0.0f,
        0.296725f, -0.409909f, -0.862513f, 0.0f,
        -0.260932f, -0.798201f, 0.542945f, 0.0f,
        -0.641628f, 0.742379f, 0.192838f, 0.0f,
        -0.186009f, -0.101514f, 0.97729f, 0.0f,
        0.106711f, -0.962067f, 0.251079f, 0.0f,
        -0.743499f, 0.30988f, -0.592607f, 0.0f,
        -0.795853f, -0.605066f, -0.0226607f, 0.0f,
        -0.828661f, -0.419471f, -0.370628f, 0.0f,
        0.0847218f, -0.489815f, -0.8677f, 0.0f,
        -0.381405f, 0.788019f, -0.483276f, 0.0f,
        0.282042f, -0.953394f, 0.107205f, 0.0f,
        0.530774f, 0.847413f, 0.0130696f, 0.0f,
        0.0515397f, 0.922524f, 0.382484f, 0.0f,
        -0.631467f, -0.709046f, 0.313852f, 0.0f,
        0.688248f, 0.517273f, 0.508668f, 0.0f,
        0.646689f, -0.333782f, -0.685845f, 0.0f,
        -0.932528f, -0.247532f, -0.262906f, 0.0f,
        0.630609f, 0.68757f, -0.359973f, 0.0f,
        0.577805f, -0.394189f, 0.714673f, 0.0f,
        -0.887833f, -0.437301f, -0.14325f, 0.0f,
        0.690982f, 0.174003f, 0.701617f, 0.0f,
        -0.866701f, 0.0118182f, 0.498689f, 0.0f,
        -0.482876f, 0.727143f, 0.487949f, 0.0f,
        -0.577567f, 0.682593f, -0.447752f, 0.0f,
        0.373768f, 0.0982991f, 0.922299f, 0.0f,
        0.170744f, 0.964243f, -0.202687f, 0.0f,
        0.993654f, -0.035791f, -0.106632f, 0.0f,
        0.587065f, 0.4143f, -0.695493f, 0.0f,
        -0.396509f, 0.26509f, -0.878924f, 0.0f,
        -0.0866853f, 0.83553f, -0.542563f, 0.0f,
        0.923193f, 0.133398f, -0.360443f, 0.0f,
        0.00379108f, -0.258618f, 0.965972f, 0.0f,
        0.239144f, 0.245154f, -0.939526f, 0.0f,
        0.758731f, -0.555871f, 0.33961f, 0.0f,
        0.295355f, 0.309513f, 0.903862f, 0.0f,
        0.0531222f, -0.91003f, -0.411124f, 0.0f,
        0.270452f, 0.0229439f, -0.96246f, 0.0f,
        0.563634f, 0.0324352f, 0.825387f, 0.0f,
        0.156326f, 0.147392f, 0.976646f, 0.0f,
        -0.0410141f, 0.981824f, 0.185309f, 0.0f,
        -0.385562f, -0.576343f, -0.720535f, 0.0f,
        0.388281f, 0.904441f, 0.176702f, 0.0f,
        0.945561f, -0.192859f, -0.262146f, 0.0f,
        0.844504f, 0.520193f, 0.127325f, 0.0f,
        0.0330893f, 0.999121f, -0.0257505f, 0.0f,
        -0.592616f, -0.482475f, -0.644999f, 0.0f,
        0.539471f, 0.631024f, -0.557476f, 0.0f,
        0.655851f, -0.027319f, -0.754396f, 0.0f,
        0.274465f, 0.887659f, 0.369772f, 0.0f,
        -0.123419f, 0.975177f, -0.183842f, 0.0f,
        -0.223429f, 0.708045f, 0.66989f, 0.0f,
        -0.908654f, 0.196302f, 0.368528f, 0.0f,
        -0.95759f, -0.00863708f, 0.288005f, 0.0f,
        0.960535f, 0.030592f, 0.276472f, 0.0f,
        -0.413146f, 0.907537f, 0.0754161f, 0.0f,
        -0.847992f, 0.350849f, -0.397259f, 0.0f,
        0.614736f, 0.395841f, 0.68221f, 0.0f,
        -0.503504f, -0.666128f, -0.550234f, 0.0f,
        -0.268833f, -0.738524f, -0.618314f, 0.0f,
        0.792737f, -0.60001f, -0.107502f, 0.0f,
        -0.637582f, 0.508144f, -0.579032f, 0.0f,
        0.750105f, 0.282165f, -0.598101f, 0.0f,
        -0.351199f, -0.392294f, -0.850155f, 0.0f,
        0.250126f, -0.960993f, -0.118025f, 0.0f,
        -0.732341f, 0.680909f, -0.0063274f, 0.0f,
        -0.760674f, -0.141009f, 0.633634f, 0.0f,
        0.222823f, -0.304012f, 0.926243f, 0.0f,
        0.209178f, 0.505671f, 0.836984f, 0.0f,
        0.757914f, -0.56629f, -0.323857f, 0.0f,
        -0.782926f, -0.339196f, 0.52151f, 0.0f,
        -0.462952f, 0.585565f, 0.665424f, 0.0f,
        0.61879f, 0.194119f, -0.761194f, 0.0f,
        0.741388f, -0.276743f, 0.611357f, 0.0f,
        0.707571f, 0.702621f, 0.0752872f, 0.0f,
        0.156562f, 0.819977f, 0.550569f, 0.0f,
        -0.793606f, 0.440216f, 0.42f, 0.0f,
        0.234547f, 0.885309f, -0.401517f, 0.0f,
        0.132598f, 0.80115f, -0.58359f, 0.0f,
        -0.377899f, -0.639179f, 0.669808f, 0.0f,
        -0.865993f, -0.396465f, 0.304748f, 0.0f,
        -0.624815f, -0.44283f, 0.643046f, 0.0f,
        -0.485705f, 0.825614f, -0.287146f, 0.0f,
        -0.971788f, 0.175535f, 0.157529f, 0.0f,
        -0.456027f, 0.392629f, 0.798675f, 0.0f,
        -0.0104443f, 0.521623f, -0.853112f, 0.0f,
        -0.660575f, -0.74519f, 0.091282f, 0.0f,
        -0.0157698f, -0.307475f, -0.951425f, 0.0f,
        -0.603467f, -0.250192f, 0.757121f, 0.0f,
        0.506876f, 0.25006f, 0.824952f, 0.0f,
        0.255404f, 0.966794f, 0.00884498f, 0.0f,
        0.466764f, -0.874228f, -0.133625f, 0.0f,
        0.475077f, -0.0682351f, -0.877295f, 0.0f,
        -0.224967f, -0.938972f, -0.260233f, 0.0f,
        -0.377929f, -0.814757f, -0.439705f, 0.0f,
        -0.305847f, 0.542333f, -0.782517f, 0.0f,
        0.26658f, -0.902905f, -0.337191f, 0.0f,
        0.0275773f, 0.322158f, -0.946284f, 0.0f,
        0.0185422f, 0.716349f, 0.697496f, 0.0f,
        -0.20483f, 0.978416f, 0.0273371f, 0.0f,
        -0.898276f, 0.373969f, 0.230752f, 0.0f,
        -0.00909378f, 0.546594f, 0.837349f, 0.0f,
        0.6602f, -0.751089f, 0.000959236f, 0.0f,
        0.855301f, -0.303056f, 0.420259f, 0.0f,
        0.797138f, 0.0623013f, -0.600574f, 0.0f,
        0.48947f, -0.866813f, 0.0951509f, 0.0f,
        0.251142f, 0.674531f, 0.694216f, 0.0f,
        -0.578422f, -0.737373f, -0.348867f, 0.0f,
        -0.254689f, -0.514807f, 0.818601f, 0.0f,
        0.374972f, 0.761612f, 0.528529f, 0.0f,
        0.640303f, -0.734271f, -0.225517f, 0.0f,
        -0.638076f, 0.285527f, 0.715075f, 0.0f,
        0.772956f, -0.15984f, -0.613995f, 0.0f,
        0.798217f, -0.590628f, 0.118356f, 0.0f,
        -0.986276f, -0.0578337f, -0.154644f, 0.0f,
        -0.312988f, -0.94549f, 0.0899272f, 0.0f,
        -0.497338f, 0.178325f, 0.849032f, 0.0f,
        -0.101136f, -0.981014f, 0.165477f, 0.0f,
        -0.521688f, 0.0553434f, -0.851339f, 0.0f,
        -0.786182f, -0.583814f, 0.202678f, 0.0f,
        -0.565191f, 0.821858f, -0.0714658f, 0.0f,
        0.437895f, 0.152598f, -0.885981f, 0.0f,
        -0.92394f, 0.353436f, -0.14635f, 0.0f,
        0.212189f, -0.815162f, -0.538969f, 0.0f,
        -0.859262f, 0.143405f, -0.491024f, 0.0f,
        0.991353f, 0.112814f, 0.0670273f, 0.0f,
        0.0337884f, -0.979891f, -0.196654f, 0.0f
    };

}
