#version 330

// The shapes of Bokeh's discs (see dev.purxiy.bokeh.BokehShape), shared by the world and the menu's preview.

const float BOKEH_PI = 3.14159265;

// How far out q lies in a disc of radius 1 of the given shape: 1 on its edge, less inside, twice as much twice as far out
// (so an edge is smoothed by how many pixels one unit is). 0 round, 1 oval (anamorphic: tall), 2 blades (six, softly
// curved, like a stopped-down aperture), 3 star (four points with curved sides: the Purxiy star), 4 heart.
float bokehGauge(vec2 q, int shape) {
    if (shape == 1) {
        return length(vec2(q.x / 0.7, q.y));
    }
    if (shape == 2) {
        const float SEGMENT = BOKEH_PI / 3.0;
        float t = mod(atan(q.y, q.x) + BOKEH_PI * 0.5, SEGMENT) - SEGMENT * 0.5;
        return length(q) * (cos(t) / cos(BOKEH_PI / 6.0) * 0.78 + 0.22);
    }
    if (shape == 3) {
        vec2 a = abs(q) + 1.0e-5;
        return pow(pow(a.x, 0.76) + pow(a.y, 0.76), 1.0 / 0.76) * 0.93;
    }
    if (shape == 4) {
        // a heart in polar form around a point above its middle, fitted into the unit circle
        vec2 h = vec2(q.x, q.y - 0.6);
        float r = length(h);
        vec2 n = h / max(r, 1.0e-6);
        float edge = 2.0 - 2.0 * n.y + n.y * sqrt(abs(n.x)) / (n.y + 1.4);
        return r / (edge * 0.37);
    }
    return length(q);
}

// How much of the unit circle a shape fills (a smaller disc of the same light is brighter).
float bokehArea(int shape) {
    return shape == 1 ? 0.70 : shape == 2 ? 0.86 : shape == 3 ? 0.53 : shape == 4 ? 0.55 : 1.0;
}

// Towards the edges of the picture a lens clips its discs from the outside (cat's eye): the disc is cut by a second one,
// shifted towards the middle, so discs there are narrower across and seem to turn around the middle. screen is where
// the disc is (0..1), aspect the picture's width over its height; swirl makes it stronger, as in old lenses.
float bokehCatsEye(vec2 q, vec2 screen, float aspect, float swirl) {
    vec2 v = (screen - 0.5) * vec2(aspect, 1.0);
    float away = length(v) / (0.5 * length(vec2(aspect, 1.0)));
    vec2 outward = v / max(length(v), 1.0e-4);
    float shift = (0.2 + 0.85 * swirl) * away * away;
    return length(q + outward * shift);
}

// The edge of every disc in red, green and blue: blue and red reach a little further out than green, as the colours of a
// real lens focus apart, which draws a soft lilac rim. radius is the disc's radius in pixels (a fringe of about a pixel
// on small discs).
vec3 bokehFringe(float fringe, float radius) {
    float f = fringe * (0.035 + 1.1 / max(radius, 1.0));
    return 1.0 + f * vec3(0.45, -0.5, 1.0);
}
