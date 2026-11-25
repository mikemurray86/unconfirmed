#!/usr/bin/env python3
"""
Generates the animation frames for the cryptids that walk or fly, as Android vector drawables.

Each creature is a small jointed rig. Walkers have a torso, a head, and legs and arms of two or three
bones. The script poses each walker at evenly spaced points through one full stride (both feet), sets
it down so its lowest foot touches the ground line, and writes one drawable per pose. Fliers are seen
from the front, with wings that bend along their length as they beat. The flap runs out from the
shoulder to the tip, so the tips trail behind on the way up and whip through on the way down. The
body bobs against the wings, and the legs and tail swing behind it. Fliers get one drawable for each
point through one wingbeat. A creature that stands still gets one for each point through a loop of
whatever it does in place, like a cloak stirring in the wind.

    app/src/main/res/drawable/cryptid_<name>_walk_<NN>.xml
    app/src/main/res/drawable/cryptid_<name>_fly_<NN>.xml
    app/src/main/res/drawable/cryptid_<name>_stand_<NN>.xml

The art follows the same layers as the other cryptid art. It's drawn facing left in a 100-unit box
with the feet at y=95. There's a dark silhouette, limbs on the far side a shade lighter so the stride
reads, fur or skin detail, moonlight along the upper-left edges, and eyeshine.

Run from the project root:  python3 tools/cryptid_frames.py [--preview DIR]
--preview also writes SVGs of every frame to DIR, for checking the art with rsvg-convert.
"""

import math
import re
import os
import sys

FRAMES = 16
GROUND = 95.0
ROOT = os.path.join(os.path.dirname(__file__), "..")
OUT = os.path.join(ROOT, "app", "src", "main", "res", "drawable")
KOTLIN = os.path.join(ROOT, "app", "src", "main", "kotlin", "app", "unconfirmed", "game", "ui", "CreatureFrames.kt")

INK = "#0B0B0B"
FAR = "#1F1C19"  # limbs on the far side, a shade lighter so they read behind the near ones
DETAIL = "#3A352F"
MOON = "#A39C8E"


def f(v):
    s = f"{v:.2f}".rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


# ---- 2D helpers ---------------------------------------------------------------------------------
# Angles are in degrees from straight down, positive swinging forward (towards -x, the way it faces).


def ray(origin, angle, length):
    a = math.radians(angle)
    return (origin[0] - math.sin(a) * length, origin[1] + math.cos(a) * length)


class Frame:
    """A local coordinate frame: a point, turned by an angle (positive tips it forward)."""

    def __init__(self, origin, angle=0.0):
        self.o = origin
        self.a = math.radians(angle)

    def __call__(self, x, y):
        c, s = math.cos(self.a), math.sin(self.a)
        # Tipping forward turns the frame anticlockwise on screen (y down, facing -x).
        return (self.o[0] + x * c + y * s, self.o[1] - x * s + y * c)


def shape(fr, cmds):
    """Path data from commands in a local frame: ('M', x, y), ('L', x, y), ('C', x1, y1, x2, y2, x, y), ('Z',)."""
    out = []
    for c in cmds:
        op, pts = c[0], c[1:]
        xy = []
        for i in range(0, len(pts), 2):
            px, py = fr(pts[i], pts[i + 1])
            xy += [f(px), f(py)]
        out.append(op + ",".join(xy) if xy else op)
    return "".join(out)


def capsule(p, q, r1, r2):
    """A limb bone: a tapered capsule from p (radius r1) to q (radius r2) with round ends."""
    dx, dy = q[0] - p[0], q[1] - p[1]
    n = math.hypot(dx, dy) or 1e-6
    ux, uy = dx / n, dy / n
    nx, ny = -uy, ux
    a = (p[0] + nx * r1, p[1] + ny * r1)
    b = (q[0] + nx * r2, q[1] + ny * r2)
    bt = (q[0] + ux * r2, q[1] + uy * r2)
    c = (q[0] - nx * r2, q[1] - ny * r2)
    d = (p[0] - nx * r1, p[1] - ny * r1)
    dt = (p[0] - ux * r1, p[1] - uy * r1)
    R1, R2 = f(r1), f(r2)
    return (
        f"M{f(a[0])},{f(a[1])}L{f(b[0])},{f(b[1])}"
        f"A{R2},{R2} 0 0 0 {f(bt[0])},{f(bt[1])}A{R2},{R2} 0 0 0 {f(c[0])},{f(c[1])}"
        f"L{f(d[0])},{f(d[1])}"
        f"A{R1},{R1} 0 0 0 {f(dt[0])},{f(dt[1])}A{R1},{R1} 0 0 0 {f(a[0])},{f(a[1])}Z"
    )


def circle(c, r):
    return f"M{f(c[0] - r)},{f(c[1])}a{f(r)},{f(r)} 0,1 1,{f(2 * r)},0a{f(r)},{f(r)} 0,1 1,{f(-2 * r)},0"


def segment(p, q, t0, t1):
    """The part of the line p→q between fractions t0 and t1, as a stroke."""
    a = (p[0] + (q[0] - p[0]) * t0, p[1] + (q[1] - p[1]) * t0)
    b = (p[0] + (q[0] - p[0]) * t1, p[1] + (q[1] - p[1]) * t1)
    return f"M{f(a[0])},{f(a[1])}L{f(b[0])},{f(b[1])}"


def side_stroke(p, q, r, t0, t1, side=1.0, inset=0.6):
    """A line along one edge of the bone p→q, `side` 1 for its forward-facing edge."""
    dx, dy = q[0] - p[0], q[1] - p[1]
    n = math.hypot(dx, dy) or 1e-6
    nx, ny = dy / n * side, -dx / n * side
    off = r - inset
    return segment((p[0] + nx * off, p[1] + ny * off), (q[0] + nx * off, q[1] + ny * off), t0, t1)


def tufts(p, q, r, count, side=1.0, length=2.2, t0=0.2, t1=0.85):
    """Short fur strokes along the edge of the bone p→q, angled down it."""
    dx, dy = q[0] - p[0], q[1] - p[1]
    n = math.hypot(dx, dy) or 1e-6
    ux, uy = dx / n, dy / n
    nx, ny = uy * side, -ux * side
    out = []
    for i in range(count):
        t = t0 + (t1 - t0) * (i / max(1, count - 1))
        bx, by = p[0] + dx * t + nx * (r - 0.4), p[1] + dy * t + ny * (r - 0.4)
        ex, ey = bx + (ux * 0.8 + nx * 0.6) * length, by + (uy * 0.8 + ny * 0.6) * length
        out.append(f"M{f(bx)},{f(by)}L{f(ex)},{f(ey)}")
    return "".join(out)


def lowest(points):
    return max(p[1] for p in points)


# ---- Gait curves ---------------------------------------------------------------------------------
# phase runs 0..2π over one full stride. At 0 the leg is under the body moving back (mid-stance);
# at π/2 it's furthest back, about to lift; at 3π/2 furthest forward, about to land.


def thigh_angle(phase, amp, lean=0.0):
    return lean - amp * math.sin(phase)


def knee_flex(phase, stance, swing):
    # The knee folds most early in the swing, just after the foot lifts off behind.
    s = max(0.0, math.sin(phase - math.pi * 0.62))
    return stance + swing * s**1.4


def in_swing(phase):
    return math.sin(phase - math.pi * 0.5) > 0  # from furthest back round to furthest forward


def biped_leg(hip, phase, thigh, shin, amp, stance_flex, swing_flex, lean=0.0):
    a = thigh_angle(phase, amp, lean)
    knee = ray(hip, a, thigh)
    sa = a - knee_flex(phase, stance_flex, swing_flex)
    ankle = ray(knee, sa, shin)
    # The foot rolls onto its toes as the leg trails behind, and hangs toes-down in the swing.
    roll = max(0.0, math.sin(phase)) ** 2 * 32 * (1 if math.cos(phase) < 0.3 else 0.6)
    if in_swing(phase):
        roll = max(roll, 18 * math.sin(phase - math.pi * 0.5) ** 0.7)
    return a, knee, sa, ankle, roll


def biped_foot(ankle, roll, heel, toe, depth):
    """Points of a flat foot under the ankle, pitched by `roll` degrees about the toes' line."""
    fr = Frame(ankle, -roll)
    return [fr(heel, depth * 0.3), fr(heel, depth), fr(-toe, depth), fr(-toe - 1.0, depth * 0.55), fr(-toe * 0.4, -0.5)]


def foot_path(pts):
    (a, b, c, d, e) = pts
    return (
        f"M{f(a[0])},{f(a[1])}L{f(b[0])},{f(b[1])}L{f(c[0])},{f(c[1])}"
        f"Q{f(d[0])},{f(d[1])} {f(e[0])},{f(e[1])}Z"
    )


# ---- Output ---------------------------------------------------------------------------------------


class Art:
    def __init__(self):
        self.layers = []

    def fill(self, color, data, alpha=None):
        # Each closed shape gets its own path. Merged into one, overlapping parts drawn in opposite
        # directions would cancel out and leave holes.
        for part in re.findall(r"M[^M]*", data):
            self.layers.append(("fill", color, part, alpha, None))

    def stroke(self, color, data, width, alpha=None):
        if data:
            self.layers.append(("stroke", color, data, alpha, width))

    def xml(self, comment):
        out = [
            '<?xml version="1.0" encoding="utf-8"?>',
            f"<!-- {comment} Generated by tools/cryptid_frames.py: edit the rig there, not this file. -->",
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
            '    android:width="100dp"',
            '    android:height="100dp"',
            '    android:viewportWidth="100"',
            '    android:viewportHeight="100">',
        ]
        for kind, color, data, alpha, width in self.layers:
            out.append("    <path")
            if kind == "fill":
                out.append(f'        android:fillColor="{color}"')
                if alpha is not None:
                    out.append(f'        android:fillAlpha="{alpha}"')
            else:
                out.append(f'        android:strokeColor="{color}"')
                if alpha is not None:
                    out.append(f'        android:strokeAlpha="{alpha}"')
                out.append(f'        android:strokeWidth="{width}"')
                out.append('        android:strokeLineCap="round"')
                out.append('        android:strokeLineJoin="round"')
            out.append(f'        android:pathData="{data}" />')
        out.append("</vector>")
        return "\n".join(out) + "\n"

    def svg(self):
        out = ['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="300" height="300">',
               '<rect width="100" height="100" fill="#6E7F86"/>',
               f'<line x1="0" y1="{GROUND}" x2="100" y2="{GROUND}" stroke="#40504f" stroke-width="0.4"/>']
        for kind, color, data, alpha, width in self.layers:
            a = 1 if alpha is None else alpha
            if kind == "fill":
                out.append(f'<path d="{data}" fill="{color}" fill-opacity="{a}"/>')
            else:
                out.append(f'<path d="{data}" fill="none" stroke="{color}" stroke-opacity="{a}" '
                           f'stroke-width="{width}" stroke-linecap="round" stroke-linejoin="round"/>')
        out.append("</svg>")
        return "\n".join(out)


def eyes(art, glow_color, centers, r_glow, r_eye):
    art.fill(glow_color, "".join(circle(c, r_glow) for c in centers), 0.3)
    art.fill(glow_color, "".join(circle(c, r_eye) for c in centers))


# ---- Bigfoot ------------------------------------------------------------------------------------
# Hunched and heavy, knees always a little bent, long arms swinging wide: the classic film stride.


def bigfoot(phase):
    thigh, shin = 23.0, 21.0
    near_leg = biped_leg((0, 0), phase, thigh, shin, 24, 12, 48, lean=4)
    far_leg = biped_leg((0, 0), phase + math.pi, thigh, shin, 24, 12, 48, lean=4)
    feet = [biped_foot(l[3], l[4], 3.5, 9.0, 4.5) for l in (near_leg, far_leg)]
    drop = GROUND - lowest(feet[0] + feet[1])
    hip = (50.0, drop)

    def place(p):
        return (p[0] + hip[0], p[1] + hip[1])

    # The body rocks a little side to side with each step and twists towards the leading leg.
    rock = 1.5 * math.sin(2 * phase)
    torso = Frame(hip, 12 + rock)
    shoulder = torso(1, -26)
    neck = torso(-6, -29)
    nod = 3 * math.sin(2 * phase + 0.8)
    head = Frame(neck, -6 + nod)

    def arm(ph):
        # ph -π/2 is the arm furthest forward, when the elbow bends most.
        fwd = max(0.0, -math.sin(ph))
        upper = -5 - 28 * math.sin(ph)
        elbow = ray(shoulder, upper, 18)
        lower = upper + 6 + 22 * fwd
        wrist = ray(elbow, lower, 17)
        hand = ray(wrist, lower + 4 + 10 * fwd, 5.5)
        return elbow, wrist, hand

    near_arm = arm(phase + math.pi)
    far_arm = arm(phase)

    art = Art()

    def leg_parts(leg, foot, color):
        _, knee, _, ankle, _ = leg
        k, a = place(knee), place(ankle)
        h = place((0, 0))
        art.fill(color, capsule(h, k, 7.2, 5.0) + capsule(k, a, 5.0, 3.3) + foot_path([place(p) for p in foot]))
        return h, k, a

    def arm_parts(a, color):
        e, w, hd = a
        art.fill(color, capsule(shoulder, e, 5.4, 4.2) + capsule(e, w, 4.2, 3.4) + capsule(w, hd, 3.6, 2.6))

    arm_parts(far_arm, FAR)
    leg_parts(far_leg, feet[1], FAR)

    body = shape(torso, [
        ("M", 7, 4), ("C", 11, -5, 12.5, -17, 9.5, -25), ("C", 7, -31, 1, -34.5, -4, -33),
        ("L", -9, -28.5), ("C", -12.5, -24, -12, -17, -9, -12), ("C", -7, -7, -7, -2, -6, 4), ("Z",),
    ])
    skull = shape(head, [
        ("M", 4, -1), ("C", 5, -8, 2, -14, -3, -15.5), ("C", -8, -16.5, -11.5, -13, -12, -9),
        ("L", -14, -7.5), ("L", -13, -5.5), ("C", -14.5, -3, -13.5, 0.5, -10.5, 2), ("C", -6, 3.5, 0, 3, 4, -1), ("Z",),
    ])
    art.fill(INK, body + skull)
    # Fur down the back and across the chest and belly, and the line of the jaw.
    art.stroke(DETAIL, "".join([
        shape(torso, [("M", 9.5, -8), ("L", 12, -5)]), shape(torso, [("M", 10.5, -15), ("L", 13, -12)]),
        shape(torso, [("M", 9.5, -22), ("L", 12.5, -20)]), shape(torso, [("M", -7.5, -10), ("C", -5, -7, -3, -8, -1, -10)]),
        shape(torso, [("M", -7, -21), ("C", -4, -18, -1, -18, 1, -21)]),
        shape(head, [("M", -10.5, -1), ("Q", -8, 0.5, -5, 0)]),
    ]), 0.8, 0.9)
    art.stroke(MOON, "".join([
        shape(head, [("M", -4, -15), ("C", -8, -16, -11, -13, -11.8, -9.5)]),
        shape(torso, [("M", -9.5, -27), ("C", -12, -23, -11.8, -17, -9.2, -12.5)]),
    ]), 0.8, 0.45)

    h, k, a = leg_parts(near_leg, feet[0], INK)
    art.stroke(DETAIL, tufts(k, a, 5.0, 3, side=-1, t0=0.1, t1=0.6), 0.8, 0.9)
    art.stroke(MOON, side_stroke(k, a, 5.0, 0.15, 0.85, 1), 0.8, 0.45)

    arm_parts(near_arm, INK)
    e, w, hd = near_arm
    art.stroke(DETAIL, tufts(e, w, 4.2, 2, side=-1, t0=0.15, t1=0.45) + tufts(shoulder, e, 5.4, 2, side=-1, t0=0.5, t1=0.85), 0.8, 0.9)
    art.stroke(MOON, side_stroke(shoulder, e, 5.4, 0.25, 0.8, 1), 0.8, 0.45)
    eyes(art, "#F2C14E", [head(-10, -6.5)], 2.3, 0.9)
    return art


# ---- Prairie Giant -------------------------------------------------------------------------------
# Very tall and thin, upright, walking stiffly with long arms barely moving: wrong in a quiet way.


def prairie_giant(phase):
    thigh, shin = 24.0, 23.0
    near_leg = biped_leg((0, 0), phase, thigh, shin, 17, 3, 34)
    far_leg = biped_leg((0, 0), phase + math.pi, thigh, shin, 17, 3, 34)
    feet = [biped_foot(l[3], l[4], 2.2, 6.0, 2.6) for l in (near_leg, far_leg)]
    drop = GROUND - lowest(feet[0] + feet[1])
    hip = (50.0, drop)

    def place(p):
        return (p[0] + hip[0], p[1] + hip[1])

    torso = Frame(hip, 3 + 0.6 * math.sin(2 * phase))
    shoulder = torso(0, -29)
    neck = torso(-0.5, -32)
    head = Frame(neck, 4 * math.sin(2 * phase - 1.2) * 0.3)

    def arm(ph):
        upper = 2 - 8 * math.sin(ph)
        elbow = ray(shoulder, upper, 20)
        lower = upper + 3 + 6 * max(0.0, -math.sin(ph))
        wrist = ray(elbow, lower, 19)
        return elbow, wrist, lower

    near_arm, far_arm = arm(phase + math.pi), arm(phase)
    art = Art()

    def arm_parts(a, color):
        e, w, lower = a
        art.fill(color, capsule(shoulder, e, 2.4, 1.8) + capsule(e, w, 1.8, 1.4))
        # Long fingers, curling a little at the tips.
        tips = []
        for spread, length in ((-9, 8.5), (-1, 9.5), (8, 8.0)):
            mid = ray(w, lower + spread, length * 0.6)
            tip = ray(mid, lower + spread + 14, length * 0.4)
            tips.append(f"M{f(w[0])},{f(w[1])}L{f(mid[0])},{f(mid[1])}L{f(tip[0])},{f(tip[1])}")
        art.stroke(color, "".join(tips), 0.9)

    def leg_parts(leg, foot, color):
        _, knee, _, ankle, _ = leg
        k, a = place(knee), place(ankle)
        art.fill(color, capsule(place((0, 0)), k, 3.4, 2.4) + capsule(k, a, 2.4, 1.6) + foot_path([place(p) for p in foot]))
        return k, a

    arm_parts(far_arm, FAR)
    leg_parts(far_leg, feet[1], FAR)
    # Narrow everywhere, the shoulders square and bony, the waist thinner than the hips.
    body = shape(torso, [
        ("M", 3.6, 3), ("C", 3, -6, 2.6, -12, 3.4, -18), ("C", 4.2, -23, 4.6, -28, 3.6, -30),
        ("L", 1, -31.5), ("L", -2, -31.5), ("L", -4.6, -30), ("C", -5.4, -27, -5, -22, -4.2, -18),
        ("C", -3.4, -12, -3.6, -6, -4, 3), ("Z",),
    ])
    skull = shape(head, [
        ("M", 1.6, 0.5), ("L", 2.4, -3), ("C", 4, -6, 3.5, -11.5, 0, -12),
        ("C", -3.5, -12, -4.5, -7, -3.6, -3.5), ("C", -3.2, -1.5, -2, 0.2, -1.2, 0.5), ("Z",),
    ])
    art.fill(INK, body + skull)
    # Ribs showing through, and a thin mouth.
    art.stroke(DETAIL, "".join([
        shape(torso, [("M", -4.4, -25), ("Q", -0.5, -23.5, 3.6, -25)]),
        shape(torso, [("M", -4.2, -21.5), ("Q", -0.5, -20, 3.4, -21.5)]),
        shape(torso, [("M", -3.8, -18), ("Q", -0.5, -16.5, 3, -18)]),
        shape(head, [("M", -2.6, -3), ("L", 0, -2.2), ("L", 2, -3)]),
    ]), 0.7, 0.9)
    art.stroke(MOON, "".join([
        shape(head, [("M", -0.5, -11.8), ("C", -3.4, -11.5, -4.4, -7.5, -3.6, -4)]),
        shape(torso, [("M", -4.4, -29.5), ("C", -5, -26, -4.6, -22, -4, -18)]),
    ]), 0.7, 0.45)
    k, a = leg_parts(near_leg, feet[0], INK)
    art.stroke(MOON, side_stroke(k, a, 2.4, 0.1, 0.9, 1, inset=0.4), 0.7, 0.45)
    arm_parts(near_arm, INK)
    e, w, _ = near_arm
    art.stroke(MOON, side_stroke(shoulder, e, 2.4, 0.15, 0.85, 1, inset=0.4), 0.7, 0.45)
    eyes(art, "#E8E2D0", [head(-3, -7), head(0.4, -7)], 1.6, 0.55)
    return art


# ---- Ringdocus -------------------------------------------------------------------------------------
# Low-slung on four legs, high at the shoulders, head carried low, and a long ringed tail that sways.


def ringdocus(phase):
    # A lateral-sequence walk: hind, fore, hind, fore, each a quarter-stride after the last.
    hind_near, fore_near = phase, phase + math.pi * 0.5
    hind_far, fore_far = phase + math.pi, phase + math.pi * 1.5

    def fore_leg(ph, shoulder):
        a = -24 * math.sin(ph)
        elbow = ray(shoulder, a, 17)
        bend = 6 + 55 * max(0.0, math.sin(ph - math.pi * 0.6)) ** 1.3
        wrist = ray(elbow, a - bend * 0.5, 16)
        paw_a = a - bend * 1.5 + 90
        toe = ray(wrist, paw_a, 4.5)
        return elbow, wrist, toe

    def hind_leg(ph, hip):
        a = 8 - 22 * math.sin(ph)
        knee = ray(hip, a + 12, 13)
        bend = 30 + 28 * max(0.0, math.sin(ph - math.pi * 0.6)) ** 1.3
        hock = ray(knee, a - bend, 12)
        ankle = ray(hock, a + 4 - bend * 0.15, 9)
        toe = ray(ankle, 90 - 0.6 * bend + a, 4.5)
        return knee, hock, ankle, toe

    # Pose everything with the body's mid-back at the origin, then lower it onto the ground.
    rise = 1.2 * math.sin(2 * phase + 0.5)
    pitch = 2.0 * math.sin(2 * phase)
    body = Frame((0, rise), pitch)
    sh, hp = body(-17, 4), body(20, 9)
    legs = {
        "fn": fore_leg(fore_near, sh), "ff": fore_leg(fore_far, sh),
        "hn": hind_leg(hind_near, hp), "hf": hind_leg(hind_far, hp),
    }
    low = lowest([p for l in legs.values() for p in l])
    off = (48.0, GROUND - 1.5 - low)

    def place(p):
        return (p[0] + off[0], p[1] + off[1])

    body = Frame(place((0, rise)), pitch)
    nod = 3.5 * math.sin(2 * phase - 0.4)
    head = Frame(body(-28, 0), -10 + nod)
    art = Art()

    def fore_parts(l, color):
        e, w, t = (place(p) for p in l)
        s = body(-17, 4)
        art.fill(color, capsule(s, e, 6.2, 3.8) + capsule(e, w, 3.6, 2.6) + capsule(w, t, 2.6, 2.3))
        return s, e, w

    def hind_parts(l, color):
        k, h, a, t = (place(p) for p in l)
        s = body(20, 9)
        art.fill(color, capsule(s, k, 7.5, 4.6) + capsule(k, h, 4.4, 2.6) + capsule(h, a, 2.6, 2.3) + capsule(a, t, 2.4, 2.1))
        return k, h

    fore_parts(legs["ff"], FAR)
    hind_parts(legs["hf"], FAR)

    # The tail: a chain of bones that sways, the wave running from the root to the tip.
    root = body(31, 1)
    angle = -62.0
    tail = [root]
    for i in range(7):
        angle += -11 + 7 * math.sin(phase - i * 0.7)
        tail.append(ray(tail[-1], angle, 3.3))
    radii = [4.0 - i * 0.42 for i in range(8)]
    art.fill(INK, "".join(capsule(tail[i], tail[i + 1], radii[i], radii[i + 1]) for i in range(7)))

    torso = shape(body, [
        ("M", -30, 6), ("C", -28, -2, -24, -8, -15, -12), ("C", -6, -15, 4, -13, 14, -9),
        ("C", 24, -5, 31, -3, 33, 3), ("C", 34, 10, 28, 15, 20, 15), ("L", -18, 14),
        ("C", -24, 13, -29, 11, -30, 6), ("Z",),
    ])
    skull = shape(head, [
        ("M", 4, -5), ("C", 0, -8, -6, -8, -10, -6), ("L", -17, -3), ("C", -20, -2, -20, 2, -17, 3),
        ("L", -8, 5), ("C", -2, 7, 4, 6, 5, 2), ("Z",),
    ])
    ear = shape(head, [("M", -3, -6.5), ("L", -1, -12), ("L", 2.5, -6), ("Z",)])
    art.fill(INK, torso + skull + ear)
    fore_parts(legs["fn"], INK)
    hk, hh = hind_parts(legs["hn"], INK)

    # A crest of bristles along the high shoulders.
    art.stroke(INK, "".join(
        shape(body, [("M", x, y), ("L", x + dx, y - 3)])
        for x, y, dx in ((-18, -10.5, -1.2), (-13, -12.5, -0.6), (-8, -13.6, 0), (-3, -13.8, 0.5), (2, -13.2, 0.9), (7, -11.8, 1.2))
    ), 0.8, 1.0)
    art.stroke(DETAIL, "".join([
        shape(head, [("M", -17, 0.5), ("L", -8, 0.5)]),
        shape(body, [("M", -12, -6), ("Q", -9, 2, -12, 10)]), shape(body, [("M", 10, -4), ("Q", 13, 3, 10, 11)]),
        shape(body, [("M", 22, 2), ("Q", 25, 6, 24, 11)]),
    ]), 0.9, 0.9)
    # Pale rings round the tail, and teeth showing under the snout.
    rings = []
    for i in range(1, 7):
        p, q = tail[i], tail[i + 1]
        mx, my = (p[0] + q[0]) / 2, (p[1] + q[1]) / 2
        dx, dy = q[0] - p[0], q[1] - p[1]
        n = math.hypot(dx, dy) or 1e-6
        r = radii[i] * 0.85
        rings.append(f"M{f(mx + dy / n * r)},{f(my - dx / n * r)}L{f(mx - dy / n * r)},{f(my + dx / n * r)}")
    art.stroke("#E8E2D0", "".join(rings), 0.9, 0.35)
    art.stroke("#E8E2D0", shape(head, [("M", -15, 3.2), ("L", -14.6, 4.6), ("M", -12, 3.8), ("L", -11.6, 5.2)]), 0.5, 0.7)
    art.stroke(MOON, "".join([
        shape(body, [("M", -27, 0), ("C", -24, -7, -17, -11, -9, -13.5)]),
        shape(head, [("M", -1, -7.5), ("C", -5, -8, -8, -7, -12, -5)]),
        side_stroke(hk, hh, 4.4, 0.1, 0.9, 1, inset=0.4),
    ]), 0.8, 0.45)
    eyes(art, "#F2C14E", [head(-11, -2.5)], 2.2, 0.8)
    return art


# ---- Gumberoo -------------------------------------------------------------------------------------
# Bigger than a bear and built like one, on four thick legs with a hump at the shoulders, but almost
# hairless: a smooth, shiny black hide where the moonlight slides along the back instead of catching
# fur. Flat, round, clawless paws, a stub of a tail, and a slow rolling walk with the head swinging
# low. Fire is the one thing that kills it, so its eyes glow like embers and a thread of smoke
# rises off its back.


def gumberoo(phase):
    hind_near, fore_near = phase, phase + math.pi * 0.5
    hind_far, fore_far = phase + math.pi, phase + math.pi * 1.5

    def fore_leg(ph, shoulder):
        a = -20 * math.sin(ph)
        elbow = ray(shoulder, a + 4, 17)
        lift = max(0.0, math.sin(ph - math.pi * 0.6)) ** 1.3
        wrist = ray(elbow, a - 10 * lift, 15)
        # The paw sits flat on the ground in stance and curls back under as it swings forward.
        toe = ray(wrist, 90 - 70 * lift, 5)
        return elbow, wrist, toe

    def hind_leg(ph, hip):
        a = 6 - 20 * math.sin(ph)
        knee = ray(hip, a + 14, 15)
        lift = max(0.0, math.sin(ph - math.pi * 0.6)) ** 1.3
        ankle = ray(knee, a - 14 - 30 * lift, 13)
        # Flat-footed like a bear: the whole sole goes down, heel and all.
        toe = ray(ankle, 90 - 55 * lift + 0.5 * a, 7)
        return knee, ankle, toe

    rise = 1.0 * math.sin(2 * phase + 0.5)
    pitch = 1.5 * math.sin(2 * phase)
    body = Frame((0, rise), pitch)
    sh, hp = body(-19, 6), body(19, 8)
    legs = {
        "fn": fore_leg(fore_near, sh), "ff": fore_leg(fore_far, sh),
        "hn": hind_leg(hind_near, hp), "hf": hind_leg(hind_far, hp),
    }
    # The paws are round, so their lowest edge is a radius below the bones.
    low = lowest([p for l in legs.values() for p in l]) + 3.0
    off = (56.0, GROUND - low)

    def place(p):
        return (p[0] + off[0], p[1] + off[1])

    body = Frame(place((0, rise)), pitch)
    # The head swings low and rises again, twice each stride, as a bear's does.
    nod = 4.0 * math.sin(2 * phase - 0.6)
    head = Frame(body(-30, 1), -6 + nod)
    art = Art()

    def fore_parts(l, color):
        e, w, t = (place(p) for p in l)
        s = body(-19, 6)
        art.fill(color, capsule(s, e, 8.0, 5.4) + capsule(e, w, 5.2, 4.0) + capsule(w, t, 3.6, 3.0))
        return e, w

    def hind_parts(l, color):
        k, a, t = (place(p) for p in l)
        s = body(19, 8)
        art.fill(color, capsule(s, k, 9.5, 5.8) + capsule(k, a, 5.4, 3.8) + capsule(a, t, 3.4, 3.0))
        return k, a

    fore_parts(legs["ff"], FAR)
    hind_parts(legs["hf"], FAR)

    torso = shape(body, [
        ("M", -33, 4), ("C", -32, -6, -26, -15, -15, -17), ("C", -6, -18.5, 4, -15, 13, -13),
        ("C", 23, -11, 31, -7, 33, 1), ("C", 34, 9, 29, 15, 21, 16), ("C", 8, 18.5, -8, 18.5, -20, 16),
        ("C", -27, 14, -33, 11, -33, 4), ("Z",),
    ])
    stub = shape(body, [("M", 31, -5), ("C", 35, -6, 37, -3, 35, 0), ("L", 32, 0), ("Z",)])
    skull = shape(head, [
        ("M", 5, -6), ("C", 2, -10, -5, -10.5, -9, -8), ("C", -12, -6, -14, -4.5, -17, -3.5),
        ("C", -19.5, -2.5, -19.5, 1.5, -17, 2.5), ("C", -13, 4, -9, 6, -4, 6.5), ("C", 2, 7, 6, 4, 5, -6), ("Z",),
    ])
    ears = circle(head(0.5, -8.5), 2.4) + circle(head(3.5, -7.5), 2.0)
    art.fill(INK, torso + stub + skull + ears)
    fore_parts(legs["fn"], INK)
    hk, ha = hind_parts(legs["hn"], INK)

    # No fur, so the detail is the hide itself: folds at the neck and behind the shoulder, and the
    # pads of the near paws.
    art.stroke(DETAIL, "".join([
        shape(body, [("M", -27, -6), ("Q", -24, 1, -26, 9)]),
        shape(body, [("M", -23, -9), ("Q", -19, -1, -21, 8)]),
        shape(body, [("M", 8, -10), ("Q", 11, 0, 7, 12)]),
        shape(head, [("M", -16, -0.5), ("L", -10, 0.5)]),
    ]), 0.9, 0.9)
    # The shine that makes it look hairless: a long highlight down the back, and more on the head,
    # the rump and the near limbs.
    art.stroke(MOON, "".join([
        shape(body, [("M", -29, -4), ("C", -26, -12, -19, -16, -10, -16.5)]),
        shape(body, [("M", -4, -16), ("C", 6, -14.5, 18, -12, 27, -8)]),
        shape(head, [("M", -2, -8.5), ("C", -6, -9, -10, -7, -14, -4.5)]),
        side_stroke(hk, ha, 5.4, 0.1, 0.9, 1, inset=0.6),
    ]), 1.1, 0.55)
    art.stroke(MOON, shape(body, [("M", -18, -13), ("C", -12, -15, -6, -15, -1, -14)]), 2.2, 0.25)
    # A thread of smoke off its back, drifting up and back as it walks.
    base = body(4, -15)
    pts = [base]
    for i in range(1, 7):
        t = i / 6
        # The curl travels up the thread once a stride, so the cycle loops cleanly.
        pts.append((base[0] + 3.5 * t + 1.6 * t * math.sin(phase - i * 0.9), base[1] - 13 * t))
    smoke = f"M{f(pts[0][0])},{f(pts[0][1])}" + "".join(
        f"Q{f(pts[i][0])},{f(pts[i][1])} {f((pts[i][0] + pts[i + 1][0]) / 2)},{f((pts[i][1] + pts[i + 1][1]) / 2)}"
        for i in range(1, 6)
    )
    art.stroke("#8A8378", smoke, 1.1, 0.5)
    eyes(art, "#E8743B", [head(-10, -4)], 2.0, 0.75)
    return art


# ---- Billiwhack Monster ------------------------------------------------------------------------------
# Upright and muscular, part goat, part ape, part man: twisted horns sweeping back off the skull, a
# goat's long face with a beard, coarse grey-white hair, and goat legs that bend back at the hock and
# walk on cloven hooves. A heavy, swaggering stride with the arms swinging.

BILLI_FUR = "#7D786F"
BILLI_FAR = "#625E57"
BILLI_DETAIL = "#56524B"
BILLI_MOON = "#D8D2C4"


def billiwhack(phase):
    thigh, shin, cannon = 18.0, 17.0, 8.0

    def leg(ph):
        a, knee, sa, ankle, roll = biped_leg((0, 0), ph, thigh, shin, 22, 14, 44, lean=2)
        # Goat legs: below the "ankle" (the hock) a long cannon bone runs forward-down to the hoof,
        # which stays flat in stance and tips toe-down in the swing.
        hoof_top = ray(ankle, sa + 28 - roll * 0.6, cannon)
        tip = ray(hoof_top, 90 - roll * 2.2, 4.0)
        return knee, ankle, hoof_top, tip, roll

    near_leg, far_leg = leg(phase), leg(phase + math.pi)
    low = lowest([p for l in (near_leg, far_leg) for p in l[:4]]) + 3.4
    hip = (52.0, GROUND - low)

    def place(p):
        return (p[0] + hip[0], p[1] + hip[1])

    rock = 1.5 * math.sin(2 * phase)
    torso = Frame(hip, 11 + rock)
    shoulder = torso(0, -25)
    neck = torso(-5, -28)
    nod = 3 * math.sin(2 * phase + 0.8)
    head = Frame(neck, 12 + nod)

    def arm(ph):
        fwd = max(0.0, -math.sin(ph))
        upper = -3 - 24 * math.sin(ph)
        elbow = ray(shoulder, upper, 15)
        lower = upper + 8 + 22 * fwd
        wrist = ray(elbow, lower, 14)
        hand = ray(wrist, lower + 6 + 10 * fwd, 5)
        return elbow, wrist, hand

    near_arm, far_arm = arm(phase + math.pi), arm(phase)
    art = Art()

    def leg_parts(l, color):
        knee, ankle, hoof_top, tip, roll = l
        h, k, a, ht = place((0, 0)), place(knee), place(ankle), place(hoof_top)
        art.fill(color, capsule(h, k, 8.0, 5.2) + capsule(k, a, 4.8, 2.8) + capsule(a, ht, 2.6, 2.1))
        # A cloven hoof: a dark wedge, split at the front.
        fr = Frame(ht, roll * 2.2 - 90 + 90)
        hoof = shape(fr, [("M", 2.0, -0.5), ("L", -2.4, -0.5), ("L", -3.4, 3.2), ("L", 2.6, 3.2), ("Z",)])
        art.fill(INK, hoof)
        art.stroke(color, shape(fr, [("M", -1.4, 1.0), ("L", -2.4, 3.3)]), 0.6)
        return k, a

    def arm_parts(a, color):
        e, w, hd = a
        art.fill(color, capsule(shoulder, e, 6.4, 4.8) + capsule(e, w, 4.6, 3.4) + capsule(w, hd, 3.6, 2.8))

    arm_parts(far_arm, BILLI_FAR)
    leg_parts(far_leg, BILLI_FAR)

    # Barrel-chested and broad at the shoulders, narrowing to the hips.
    body = shape(torso, [
        ("M", 7, 4), ("C", 11, -5, 13.5, -16, 10.5, -25), ("C", 7.5, -30.5, 0, -33, -6, -31.5),
        ("L", -11, -27.5), ("C", -16, -22, -15, -14, -10.5, -8), ("C", -8, -4, -7.5, 0, -7, 4), ("Z",),
    ])
    # A long goat's face, sloping down to the muzzle, with a beard under the chin.
    skull = shape(head, [
        ("M", 4, -2), ("C", 5, -8, 2, -12, -3, -12), ("C", -7, -12, -9, -9, -11, -5),
        ("L", -14, 1), ("C", -15, 3.5, -13.5, 5, -11, 4.5), ("C", -7, 4, -3, 3, 0, 2), ("C", 2, 1, 3.5, 0, 4, -2), ("Z",),
    ])
    beard = shape(head, [("M", -10, 4), ("L", -10.5, 10), ("L", -7.5, 4.5), ("L", -6, 8), ("L", -5, 3.5), ("Z",)])
    ear = shape(head, [("M", 1, -7), ("L", 7.5, -5), ("L", 1.5, -4), ("Z",)])
    art.fill(BILLI_FUR, body + skull + beard + ear)

    # Twisted horns: a tapering chain that sweeps back and curls down, ridged along its length.
    horn = [head(-2, -11)]
    angle = 160.0
    for i in range(8):
        angle += 24
        horn.append(ray(horn[-1], angle - 12 + nod, 2.6))
    hr = [2.0 - i * 0.2 for i in range(9)]
    art.fill(INK, "".join(capsule(horn[i], horn[i + 1], hr[i], hr[i + 1]) for i in range(8)))
    ridges = []
    for i in range(1, 7):
        p, q = horn[i], horn[i + 1]
        dx, dy = q[0] - p[0], q[1] - p[1]
        n = math.hypot(dx, dy) or 1e-6
        r = hr[i] * 0.9
        ridges.append(f"M{f(p[0] + dy / n * r)},{f(p[1] - dx / n * r)}L{f(p[0] - dy / n * r * 0.6 + dx * 0.3)},{f(p[1] + dx / n * r * 0.6 + dy * 0.3)}")
    art.stroke(BILLI_DETAIL, "".join(ridges), 0.6, 0.9)

    art.stroke(BILLI_DETAIL, "".join([
        shape(torso, [("M", 8, -8), ("L", 10.5, -5)]), shape(torso, [("M", 9, -15), ("L", 11.5, -12)]),
        shape(torso, [("M", 8, -22), ("L", 11, -20)]), shape(torso, [("M", -8.5, -21), ("C", -5, -17, -1, -17, 2, -21)]),
        shape(torso, [("M", -7.5, -11), ("C", -5, -8, -2, -8, 0, -11)]),
        shape(head, [("M", -13, 1.5), ("Q", -10, 2.5, -7, 2)]),
    ]), 0.8, 0.9)
    art.stroke(BILLI_MOON, "".join([
        shape(head, [("M", -3, -11.5), ("C", -7, -11.5, -9, -8.5, -10.8, -5)]),
        shape(torso, [("M", -9.5, -26), ("C", -13, -21, -12.5, -14, -9, -8.5)]),
    ]), 0.8, 0.5)
    k, a = leg_parts(near_leg, BILLI_FUR)
    art.stroke(BILLI_DETAIL, tufts(place((0, 0)), k, 6.5, 3, side=-1, t0=0.3, t1=0.8), 0.8, 0.9)
    art.stroke(BILLI_MOON, side_stroke(place((0, 0)), k, 7.0, 0.2, 0.85, 1), 0.8, 0.5)
    arm_parts(near_arm, BILLI_FUR)
    e, w, _ = near_arm
    art.stroke(BILLI_DETAIL, tufts(shoulder, e, 5.6, 2, side=-1, t0=0.5, t1=0.85), 0.8, 0.9)
    art.stroke(BILLI_MOON, side_stroke(shoulder, e, 5.6, 0.25, 0.8, 1), 0.8, 0.5)
    eyes(art, "#F2C14E", [head(-6, -6.5)], 2.0, 0.75)
    return art


# ---- Fresno Nightcrawlers ----------------------------------------------------------------------------
# Always seen in pairs: two small white figures that are nearly all legs, a small blank head sitting
# right on top of the hips, no arms and hardly any body. They walk with a stiff, smooth glide, legs
# swinging from the hip with the knees barely bending and the head never bobbing. The second walks a
# little behind and further off, out of step with the first.

CRAWLER = "#E9E6DC"
CRAWLER_FAR = "#BDB8AC"
CRAWLER_DETAIL = "#A8A397"


def nightcrawler_one(art, phase, x, ground, scale, fade):
    thigh, shin = 21.0 * scale, 22.0 * scale
    legs = [biped_leg((0, 0), ph, thigh, shin, 19, 2, 22) for ph in (phase, phase + math.pi)]
    # The legs end in points: whatever prints it leaves are tiny.
    toes = [ray(l[3], l[2] + 20, 2.5 * scale) for l in legs]
    low = lowest([l[3] for l in legs] + toes) + 0.8 * scale
    hip = (x, ground - low)

    def place(p):
        return (p[0] + hip[0], p[1] + hip[1])

    def leg_parts(l, toe, color):
        k, a, t = place(l[1]), place(l[3]), place(toe)
        art.fill(color, capsule(hip, k, 3.0 * scale, 2.0 * scale) + capsule(k, a, 2.0 * scale, 1.2 * scale)
                 + capsule(a, t, 1.2 * scale, 0.7 * scale), fade)
        return k, a

    leg_parts(legs[1], toes[1], CRAWLER_FAR)
    # A small round head set right on the hips, with only a stub of a waist between.
    head = Frame(hip, 0)
    blob = shape(head, [
        ("M", 0, 2.5 * scale), ("C", 4 * scale, 2.5 * scale, 4.5 * scale, -2 * scale, 3.8 * scale, -6 * scale),
        ("C", 3 * scale, -10.5 * scale, -3 * scale, -10.5 * scale, -3.8 * scale, -6 * scale),
        ("C", -4.5 * scale, -2 * scale, -4 * scale, 2.5 * scale, 0, 2.5 * scale), ("Z",),
    ])
    art.fill(CRAWLER, blob, fade)
    k, a = leg_parts(legs[0], toes[0], CRAWLER)
    art.stroke(CRAWLER_DETAIL, shape(head, [("M", -3 * scale, -1 * scale), ("Q", 0, 0.5 * scale, 3 * scale, -1 * scale)])
               + side_stroke(k, a, 2.0 * scale, 0.1, 0.9, -1, inset=0.5), 0.6, 0.8 if fade is None else 0.8 * fade)


def nightcrawler(phase):
    art = Art()
    # The one behind is drawn first, smaller and higher up the ground, a little out of step.
    nightcrawler_one(art, phase + 0.9, 63.0, GROUND - 4, 0.82, 0.85)
    nightcrawler_one(art, phase, 40.0, GROUND, 1.0, None)
    return art


# ---- Lake Chelan Monster ------------------------------------------------------------------------------
# A pale dragon that hauls itself out of the lake: built like an alligator, low on four sprawling
# legs with clawed feet, a narrow serpent's head on a long neck, a ridge of plates down the back and a
# long scaly tail that swings as it walks. The 1892 story calls it white.

CHELAN = "#A9A496"
CHELAN_FAR = "#858071"
CHELAN_DETAIL = "#6F6A5E"
CHELAN_MOON = "#E2DDD0"


def lake_chelan(phase):
    hind_near, fore_near = phase, phase + math.pi * 0.5
    hind_far, fore_far = phase + math.pi, phase + math.pi * 1.5

    def sprawl_leg(ph, root, upper, lower, reach):
        # A reptile's leg: the upper bone juts out and up from the body, so the elbow or knee sits
        # high, and the lower bone comes down to a foot planted ahead of or behind it.
        swing = -reach * math.sin(ph)
        lift = max(0.0, math.sin(ph - math.pi * 0.6)) ** 1.3
        elbow = ray(root, swing - 40, upper)
        foot = ray(elbow, swing * 0.6 + 12 - 10 * lift, lower - 4 * lift)
        toe = ray(foot, 90 - 30 * lift, 4.5)
        return elbow, foot, toe

    # The body sways in a long wave from head to tail, seen from the side as a gentle rise and fall.
    rise = 0.8 * math.sin(2 * phase + 0.3)
    body = Frame((0, rise), 0)
    sh, hp = body(-14, 3), body(14, 3)
    legs = {
        "fn": sprawl_leg(fore_near, sh, 9, 12, 26), "ff": sprawl_leg(fore_far, sh, 9, 12, 26),
        "hn": sprawl_leg(hind_near, hp, 10, 13, 24), "hf": sprawl_leg(hind_far, hp, 10, 13, 24),
    }
    low = lowest([p for l in legs.values() for p in l]) + 2.4
    off = (38.0, GROUND - low)

    def place(p):
        return (p[0] + off[0], p[1] + off[1])

    body = Frame(place((0, rise)), 0)
    art = Art()

    def leg_parts(l, root, color, r):
        e, ft, t = (place(p) for p in l)
        s = body(*root)
        art.fill(color, capsule(s, e, r, r * 0.75) + capsule(e, ft, r * 0.75, r * 0.5) + capsule(ft, t, r * 0.45, r * 0.35))
        # Three claws spread from the foot.
        claws = "".join(
            f"M{f(t[0])},{f(t[1])}L{f(ray(t, 90 + d, 2.6)[0])},{f(ray(t, 90 + d, 2.6)[1])}" for d in (-18, 0, 18)
        )
        art.stroke(CHELAN_DETAIL, claws, 0.9)
        return e, ft

    leg_parts(legs["ff"], (-14, 3), CHELAN_FAR, 4.4)
    leg_parts(legs["hf"], (14, 3), CHELAN_FAR, 5.2)

    # The tail: a long tapering chain whose swing travels from the root to the tip.
    tail = [body(20, -1)]
    angle = -86.0
    for i in range(10):
        angle += 1.2 + 4.5 * math.sin(phase - i * 0.6)
        tail.append(ray(tail[-1], angle, 3.4))
    tr = [5.2 - i * 0.46 for i in range(11)]
    art.fill(CHELAN, "".join(capsule(tail[i], tail[i + 1], tr[i], tr[i + 1]) for i in range(10)))

    # A long, low, heavy body, and a neck reaching forward to a narrow head that swings as it walks.
    torso = shape(body, [
        ("M", -22, 0), ("C", -21, -6, -14, -9, -4, -9.5), ("C", 8, -10, 18, -8, 23, -3),
        ("C", 25, 1, 22, 6, 15, 6.5), ("L", -14, 6.5), ("C", -20, 6, -23, 4, -22, 0), ("Z",),
    ])
    sway = 3 * math.sin(phase * 2 + 1.0)
    neck_base = body(-20, -1)
    neck_tip = ray(neck_base, -78 + sway * 0.4, 9)
    head = Frame(neck_tip, -4 + sway)
    neck = capsule(neck_base, neck_tip, 5.2, 3.6)
    skull = shape(head, [
        ("M", 2, -3.5), ("C", -2, -4.8, -7, -4.5, -11, -3), ("L", -16, -1.2), ("C", -17.5, -0.6, -17.5, 1, -16, 1.4),
        ("L", -10, 2.6), ("C", -5, 3.4, 0, 3.2, 2.5, 2), ("C", 3.5, 0.5, 3.5, -2.5, 2, -3.5), ("Z",),
    ])
    art.fill(CHELAN, torso + neck + skull)
    # A row of plates down the back and onto the tail.
    plates = []
    for x in range(-16, 21, 4):
        top = body(x, -9.6 + 0.004 * x * x)
        plates.append(poly([(top[0] - 1.6, top[1] + 0.6), (top[0], top[1] - 2.6), (top[0] + 1.6, top[1] + 0.6)]))
    for i in range(1, 7):
        p, q = tail[i], tail[i + 1]
        dx, dy = q[0] - p[0], q[1] - p[1]
        n = math.hypot(dx, dy) or 1e-6
        nx, ny = dy / n, -dx / n
        if ny > 0:
            nx, ny = -nx, -ny
        r, h = tr[i], 2.4 - i * 0.25
        plates.append(poly([(p[0] + nx * r, p[1] + ny * r), (p[0] + nx * (r + h) + dx * 0.25, p[1] + ny * (r + h) + dy * 0.25),
                            (q[0] + nx * tr[i + 1], q[1] + ny * tr[i + 1])]))
    art.fill(CHELAN_DETAIL, "".join(plates))

    leg_parts(legs["fn"], (-14, 3), CHELAN, 4.4)
    leg_parts(legs["hn"], (14, 3), CHELAN, 5.2)
    # Scales as rows of little arcs along the flank, the jaw line and teeth.
    scales = []
    for row, y in enumerate((-5, -1.5, 2)):
        for x in range(-16 + 2 * (row % 2), 20, 4):
            a = body(x - 1.4, y)
            b = body(x + 1.4, y)
            c = body(x, y + 1.6)
            scales.append(f"M{f(a[0])},{f(a[1])}Q{f(c[0])},{f(c[1])} {f(b[0])},{f(b[1])}")
    art.stroke(CHELAN_DETAIL, "".join(scales) + shape(head, [("M", -16, 0.4), ("L", -3, 1.2)]), 0.6, 0.9)
    art.stroke("#F4F1E8", shape(head, [("M", -14, 0.8), ("L", -13.6, 2.1), ("M", -11, 1.1), ("L", -10.6, 2.4),
                                     ("M", -8, 1.3), ("L", -7.6, 2.6)]), 0.5, 0.8)
    art.stroke(CHELAN_MOON, "".join([
        shape(body, [("M", -20, -3.5), ("C", -16, -8.5, -8, -9.6, 2, -9.6)]),
        shape(head, [("M", 0, -3.8), ("C", -4, -4.6, -8, -4.2, -12, -2.6)]),
    ]), 0.8, 0.55)
    eyes(art, "#C8E05A", [head(-7.5, -1.6)], 1.6, 0.6)
    return art


# ---- Batsquatch -------------------------------------------------------------------------------------
# A winged ape, nine feet tall: a heavy, broad-chested body in bluish fur, with arms that hang and
# swing as well as great leathery bat wings, short bowed legs dangling, a heavy-browed face with a
# jutting muzzle, and yellow eyes. Its beats are slow and laboured for something so heavy.

BAT_FUR = "#1C2333"
BAT_FUR_DETAIL = "#3C4864"
BAT_WING_SHAPE = [(0, -2), (10, -4.5), (19, -7), (30, -5), (44, -3), (37, 3.5), (38, 12), (31.5, 11.5),
                  (27, 19), (19.5, 14), (11, 18), (5, 14.5), (0, 13)]
BAT_WING_FINGERS = [[(1, -1.5), (19, -6)], [(19, -6), (43, -3)], [(19, -6), (37.5, 11)], [(19, -6), (27, 17.5)]]


def batsquatch(phase):
    bob = 3.8 * math.sin(phase - 0.7)
    angle = wingbeat(phase, 14, 34, 44, 1.0, droop=9)
    chord = 0.84 - 0.16 * math.cos(phase)
    wl, wr = pair((41, 33 + bob), (59, 33 + bob), angle, chord)
    art = Art()
    art.fill(INK, wl.poly(BAT_WING_SHAPE) + wr.poly(BAT_WING_SHAPE))
    art.stroke(DETAIL, "".join(w.line(b) for w in (wl, wr) for b in BAT_WING_FINGERS), 0.9, 0.9)
    art.stroke(INK, "".join(w.line([(19, -6), (18, -10), (16.5, -11)], 3) for w in (wl, wr)), 1.2)
    art.stroke(MOON, wl.line([(1, -2), (10, -4.6), (19, -7.1), (30, -5.1), (43.5, -3.1)]), 0.8, 0.45)
    art.stroke(MOON, wr.line([(1, -2), (10, -4.6), (19, -7.1), (30, -5.1)]), 0.7, 0.25)

    # Short, heavy legs dangling a beat behind the body.
    swing = 4 * math.sin(phase - 1.6)
    for side in (-1, 1):
        hip = (50 + side * 6, 64 + bob)
        knee, ankle = dangle(hip, side * (6 + 2 * math.sin(phase - 2.0)) + swing * 0.4, 11, 11, 6 + swing)
        foot = (ankle[0] + side * 1.5, ankle[1] + 2.5)
        art.fill(BAT_FUR, capsule(hip, knee, 5.4, 4.0) + capsule(knee, ankle, 4.0, 3.0) + capsule(ankle, foot, 3.2, 2.8))
    # The arms hang below the wings and swing with the beat, big hands open.
    reach = 6 * math.sin(phase - 1.2)
    arms = []
    for side in (-1, 1):
        sh = (50 + side * 9.5, 38 + bob)
        el = (50 + side * (14 + reach * 0.3), 50 + bob)
        wr_ = (50 + side * (13 + reach * 0.5), 61 + bob - reach * 0.4)
        hand = (wr_[0] + side * 0.5, wr_[1] + 4.5)
        arms.append((sh, el, wr_, hand))
        art.fill(BAT_FUR, capsule(sh, el, 4.4, 3.6) + capsule(el, wr_, 3.6, 3.0) + capsule(wr_, hand, 3.0, 2.6))
    body = [("M", 50, 26), ("C", 55, 26, 58, 29, 60, 33), ("C", 63, 36, 63, 44, 61, 52), ("C", 60, 58, 58.5, 63, 57, 67),
            ("L", 43, 67), ("C", 41.5, 63, 40, 58, 39, 52), ("C", 37, 44, 37, 36, 40, 33), ("C", 42, 29, 45, 26, 50, 26), ("Z",)]
    head = [("M", 50, 16), ("C", 54.5, 16, 57, 19.5, 56.5, 24), ("C", 56, 28, 54, 31, 50, 31.5), ("C", 46, 31, 44, 28, 43.5, 24),
            ("C", 43, 19.5, 45.5, 16, 50, 16), ("Z",)]
    ear = [("M", 45, 19.5), ("L", 42, 13.5), ("L", 47, 17.5), ("Z",)]
    art.fill(BAT_FUR, path(body, 0, bob) + path(head, 0, bob) + path(ear, 0, bob) + path(mirror_x(ear), 0, bob))
    # A heavy brow, a jutting muzzle, fur on the chest, shoulders and arms.
    art.fill(INK, path([("M", 46.5, 25.5), ("C", 47, 29.5, 53, 29.5, 53.5, 25.5), ("C", 52, 26.5, 48, 26.5, 46.5, 25.5), ("Z",)], 0, bob))
    art.stroke(BAT_FUR_DETAIL, path([("M", 45.5, 21), ("Q", 50, 19.5, 54.5, 21), ("M", 44.5, 38), ("L", 46, 41.5), ("M", 50, 37.5),
                                     ("L", 50, 41.5), ("M", 55.5, 38), ("L", 54, 41.5), ("M", 43, 47), ("Q", 50, 50, 57, 47),
                                     ("M", 43.5, 55), ("Q", 50, 57.5, 56.5, 55)], 0, bob)
               + "".join(tufts(a[1], a[2], 3.6, 2, side=-1 if a[0][0] < 50 else 1, t0=0.2, t1=0.7) for a in arms), 0.8, 0.9)
    art.stroke(MOON, path([("M", 45, 18.5), ("C", 46.5, 16.5, 48, 16, 50, 16), ("M", 40, 34), ("C", 37.5, 40, 37.6, 47, 39, 52)], 0, bob),
               0.8, 0.45)
    eyes(art, "#F2C14E", [(47.4, 23 + bob), (52.6, 23 + bob)], 2.2, 0.85)
    return art


# ---- Jackalope ------------------------------------------------------------------------------------
# A jackrabbit with a small rack of antlers, bounding: it gathers its hind feet up under it, pushes
# off and stretches out long in the air with its ears laid back, lands on its front paws, and swings
# its hind feet past them to land again. Tawny-grey fur, a white scut and black-tipped ears.

JACK_FUR = "#6B5D4F"
JACK_FAR = "#4F4439"
JACK_DETAIL = "#3E352C"
JACK_MOON = "#D8CBB4"


def jackalope(phase):
    t = (phase / (2 * math.pi)) % 1.0
    # How stretched out it is: 1 at full stretch in the air, -1 gathered up on landing.
    s = math.sin(2 * math.pi * (t - 0.1))
    # Off the ground from push-off to the front paws landing.
    lift = 7.0 * max(0.0, math.sin(math.pi * (t - 0.18) / 0.42)) if 0.18 <= t <= 0.6 else 0.0
    # Nose up as it pushes off, nose down as it lands.
    pitch = 10 * math.sin(2 * math.pi * (t - 0.02))
    stretch = 1 + 0.1 * s

    body = Frame((0, 0), pitch)

    def hind_leg():
        # Gathered: thigh forward and the long foot flat under the body. Stretched: the whole leg
        # trails out behind, foot pointing back.
        a = 25 - 60 * (0.5 + 0.5 * s)
        hip = body(9 * stretch, 2)
        knee = ray(hip, a + 35, 9)
        ankle = ray(knee, a - 40, 8)
        toe = ray(ankle, a + 80 - 40 * (0.5 + 0.5 * s), 9)
        return hip, knee, ankle, toe

    def fore_leg(off):
        # Stretched: reaching out ahead to land. Gathered: tucked back under the chest.
        a = -20 + 70 * (0.5 + 0.5 * s) + off
        sh = body(-9 * stretch, 2)
        elbow = ray(sh, a, 6.5)
        paw = ray(elbow, a - 15, 6.5)
        toe = ray(paw, a + 70, 3)
        return sh, elbow, paw, toe

    hind = hind_leg()
    fores = [fore_leg(8), fore_leg(-6)]
    low = lowest(list(hind[1:]) + [p for l in fores for p in l[1:]]) + 1.5
    off = (52.0, GROUND - low - lift)

    def place(p):
        return (p[0] + off[0], p[1] + off[1])

    body = Frame(place((0, 0)), pitch)
    head = Frame(body(-14 * stretch, -5), -12 + 8 * s)
    art = Art()

    # Far-side legs first: a second hind leg just behind the near one, and the far foreleg.
    h, k, a, tp = (place(p) for p in hind)
    far_shift = (1.5, -0.8)
    art.fill(JACK_FAR, capsule((h[0] + far_shift[0], h[1] + far_shift[1]), (k[0] + far_shift[0], k[1] + far_shift[1]), 4.5, 2.6)
             + capsule((k[0] + far_shift[0], k[1] + far_shift[1]), (a[0] + far_shift[0], a[1] + far_shift[1]), 2.4, 1.6)
             + capsule((a[0] + far_shift[0], a[1] + far_shift[1]), (tp[0] + far_shift[0], tp[1] + far_shift[1]), 1.8, 1.4))
    s1, e1, p1, t1 = (place(p) for p in fores[1])
    art.fill(JACK_FAR, capsule(s1, e1, 2.2, 1.6) + capsule(e1, p1, 1.6, 1.2) + capsule(p1, t1, 1.3, 1.1))

    # Ears laid back along the neck in the air, raised when it's gathered.
    ear_a = 200 + 35 * (0.5 + 0.5 * s)
    ear_root = head(2, -3.5)
    ear_tip = ray(ear_root, ear_a, 12)
    ear2_root = head(3.5, -3)
    ear2_tip = ray(ear2_root, ear_a + 10, 11)
    art.fill(JACK_FAR, capsule(ear2_root, ear2_tip, 1.7, 1.3))

    torso = shape(Frame(body.o, math.degrees(body.a)), [
        ("M", -13 * stretch, 1), ("C", -13 * stretch, -5, -7, -8, 0, -8), ("C", 7, -8, 13 * stretch, -6, 13.5 * stretch, 0),
        ("C", 14 * stretch, 5, 9, 7, 2, 6.5), ("L", -8, 5.5), ("C", -12 * stretch, 5, -13 * stretch, 3.5, -13 * stretch, 1), ("Z",),
    ])
    skull = shape(head, [
        ("M", 4, -1), ("C", 4, -4.5, 0, -5.5, -3, -5), ("C", -6, -4.5, -8.5, -2.5, -9, 0),
        ("C", -9.2, 1.8, -7.5, 3, -5, 3), ("C", -1, 3.2, 4, 2.5, 4, -1), ("Z",),
    ])
    art.fill(JACK_FUR, torso + skull + capsule(ear_root, ear_tip, 2.0, 1.4))
    # Black ear tips and the white scut.
    art.fill(INK, capsule(ray(ear_root, ear_a, 10.2), ear_tip, 1.5, 1.4))
    art.fill("#E6E0D2", circle(body(13.5 * stretch, -2.5), 2.6))

    # Antlers: a small rack in pale old bone, rooted on the crown in front of the ears. Each beam
    # rises and curves forward, with two tines off its front. The far one sits a little behind.
    for dx, shade in ((1.4, "#9C907A"), (0.0, "#CDBF9F")):
        base = head(-1.5 + dx, -4.6)
        beam = [base]
        ang = 196.0
        for _ in range(5):
            ang -= 9
            beam.append(ray(beam[-1], ang, 2.6))
        art.fill(shade, "".join(capsule(beam[i], beam[i + 1], 1.05 - i * 0.12, 0.93 - i * 0.12) for i in range(5)))
        art.fill(shade, capsule(beam[1], ray(beam[1], 120, 3.0), 0.7, 0.4) + capsule(beam[3], ray(beam[3], 112, 3.4), 0.65, 0.35))
        art.stroke(JACK_DETAIL, "".join(segment(beam[i], beam[i + 1], 0, 1) for i in range(5)), 0.35, 0.6)

    hk = place(hind[1])
    art.fill(JACK_FUR, capsule(h, k, 5.0, 2.8) + capsule(k, a, 2.6, 1.7) + capsule(a, tp, 2.0, 1.5))
    s0, e0, p0, t0 = (place(p) for p in fores[0])
    art.fill(JACK_FUR, capsule(s0, e0, 2.4, 1.7) + capsule(e0, p0, 1.7, 1.3) + capsule(p0, t0, 1.4, 1.2))

    art.stroke(JACK_DETAIL, "".join([
        shape(head, [("M", -8.5, 0.8), ("L", -6, 1.4)]),
        shape(Frame(body.o, math.degrees(body.a)), [("M", -6, -2), ("Q", -4, 2, -6, 5)]),
        shape(Frame(body.o, math.degrees(body.a)), [("M", 5, -5), ("Q", 8, 0, 5, 6)]),
    ]) + tufts(h, hk, 4.5, 3, side=-1, length=1.6), 0.6, 0.9)
    art.stroke(JACK_MOON, "".join([
        shape(Frame(body.o, math.degrees(body.a)), [("M", -11, -4), ("C", -8, -7.5, -3, -8.2, 3, -7.8)]),
        shape(head, [("M", 1, -4.8), ("C", -2, -5.4, -5, -4.4, -7.5, -2)]),
    ]) + side_stroke(ear_root, ray(ear_root, ear_a, 8), 1.9, 0.1, 0.9, 1, inset=0.3), 0.7, 0.5)
    eyes(art, "#E8743B", [head(-4.5, -1.5)], 1.5, 0.6)
    return art


# ---- The Wimpus ------------------------------------------------------------------------------------
# The thing pulled out of Lake De Smet in 1915: about two feet long and the color of lead, a broad,
# blunt head with three "horns" sweeping back on each side (a water dog's feathery gills), a long
# flat-sided tail, and four small sprawling legs. It crawls with a wriggle, and the tail swings.

WIMPUS = "#6A6E72"
WIMPUS_FAR = "#4E5257"
WIMPUS_DETAIL = "#3D4145"
WIMPUS_MOON = "#C9CDD2"
WIMPUS_GILL = "#8E9398"  # a shade paler than the hide, so the gills read against the body


def wimpus(phase):
    hind_near, fore_near = phase, phase + math.pi
    hind_far, fore_far = phase + math.pi, phase

    def leg(ph, root, upper, lower, reach):
        swing = -reach * math.sin(ph)
        lift = max(0.0, math.sin(ph - math.pi * 0.6)) ** 1.3
        elbow = ray(root, swing - 70, upper)
        foot = ray(elbow, swing * 0.5 + 20 - 12 * lift, lower - 1.5 * lift)
        toe = ray(foot, 90 - 30 * lift, 3)
        return elbow, foot, toe

    # The body bends in a wave as it crawls; seen side-on, the back rises and falls a little.
    rise = 0.6 * math.sin(2 * phase)
    body = Frame((0, rise), 1.5 * math.sin(phase))
    sh, hp = body(-11, 3), body(9, 3)
    legs = {
        "fn": leg(fore_near, sh, 4.5, 5, 40), "ff": leg(fore_far, sh, 4.5, 5, 40),
        "hn": leg(hind_near, hp, 5, 5.5, 38), "hf": leg(hind_far, hp, 5, 5.5, 38),
    }
    low = lowest([p for l in legs.values() for p in l]) + 1.4
    off = (40.0, GROUND - low)

    def place(p):
        return (p[0] + off[0], p[1] + off[1])

    body = Frame(place((0, rise)), 1.5 * math.sin(phase))
    art = Art()

    def leg_parts(l, root, color):
        e, ft, t = (place(p) for p in l)
        s = body(*root)
        art.fill(color, capsule(s, e, 3.0, 2.2) + capsule(e, ft, 2.2, 1.6) + capsule(ft, t, 1.5, 1.1))
        art.stroke(color, "".join(f"M{f(t[0])},{f(t[1])}L{f(ray(t, 90 + d, 1.8)[0])},{f(ray(t, 90 + d, 1.8)[1])}"
                                  for d in (-25, 0, 25)), 0.7)

    leg_parts(legs["ff"], (-11, 3), WIMPUS_FAR)
    leg_parts(legs["hf"], (9, 3), WIMPUS_FAR)

    # A long tail, flattened like a paddle, swinging as it crawls.
    tail = [body(15, 0)]
    ang = -88.0
    for i in range(9):
        ang += 1.0 + 6 * math.sin(phase - i * 0.7)
        tail.append(ray(tail[-1], ang, 3.4))
    tr = [4.6 - i * 0.45 for i in range(10)]
    art.fill(WIMPUS, "".join(capsule(tail[i], tail[i + 1], tr[i], tr[i + 1]) for i in range(9)))

    torso = shape(body, [
        ("M", -16, 0), ("C", -15, -5, -9, -7, -1, -7), ("C", 8, -7, 15, -5, 17, -1),
        ("C", 18, 3, 14, 5.5, 8, 5.5), ("L", -10, 5.5), ("C", -15, 5, -17, 3, -16, 0), ("Z",),
    ])
    head = Frame(body(-16, -0.5), -3 + 3 * math.sin(phase + 0.8))
    # Broad and blunt: wider than the neck and flat across the snout.
    skull = shape(head, [
        ("M", 2, -5), ("C", -3, -6.5, -9, -6, -11.5, -3.5), ("C", -13, -1.5, -13, 1.5, -11.5, 3),
        ("C", -8, 5, -2, 5, 2, 4), ("Z",),
    ])
    art.fill(WIMPUS, torso + skull)

    # Three feathery "horns" on the near side, sweeping back from behind the head and waving a
    # little; the far side's show as darker stubs above them.
    def gill(root_y, angle, length, wave, color, width):
        # A stalk sweeping back from behind the jaw, curling as it goes, fringed like a feather.
        root = head(2, root_y)
        pts = [root]
        a = angle
        for i in range(5):
            a += 1.5 + 3 * math.sin(phase * 2 + wave + i * 0.8)
            pts.append(ray(pts[-1], a, length / 5))
        art.fill(color, "".join(capsule(pts[i], pts[i + 1], width - i * 0.18, width - (i + 1) * 0.18) for i in range(5)))
        fringe = ""
        for i in range(1, 5):
            p, q = pts[i], pts[i + 1]
            dx, dy = q[0] - p[0], q[1] - p[1]
            n = math.hypot(dx, dy) or 1e-6
            for side in (-1, 1):
                tip = (p[0] + dy / n * 2.0 * side + dx * 0.35, p[1] - dx / n * 2.0 * side + dy * 0.35)
                fringe += f"M{f(p[0])},{f(p[1])}L{f(tip[0])},{f(tip[1])}"
        art.stroke(color, fringe, 0.55)

    # The far side's three, darker, then the near side's three fanned out: up and back, straight
    # back, and back and down.
    for y, angle, wave in ((-5, 222, 0.4), (-3, 244, 1.4), (-1, 264, 2.4)):
        gill(y - 0.8, angle - 6, 11, wave, WIMPUS_FAR, 1.3)
    for y, angle, wave in ((-4.5, 226, 0.0), (-2.2, 250, 1.0), (0.2, 272, 2.0)):
        gill(y, angle, 12.5, wave, WIMPUS_GILL, 1.5)

    leg_parts(legs["fn"], (-11, 3), WIMPUS)
    leg_parts(legs["hn"], (9, 3), WIMPUS)
    # Mottling, the wide mouth line, a sheen of slime along the back.
    spots = "".join(circle(body(x, y), r) for x, y, r in ((-6, -3, 1.0), (1, -4.5, 0.8), (6, -2, 1.1), (11, -4, 0.7), (-2, 1, 0.8)))
    art.fill(WIMPUS_DETAIL, spots, 0.8)
    art.stroke(WIMPUS_DETAIL, shape(head, [("M", -12, 1), ("Q", -7, 2.5, -1, 2)]), 0.7, 0.9)
    art.stroke(WIMPUS_MOON, shape(body, [("M", -14, -3.5), ("C", -10, -6.6, -2, -7.2, 8, -6.6)])
               + shape(head, [("M", 0, -5.5), ("C", -4, -6.2, -8, -5.4, -10.5, -3.6)]), 0.8, 0.6)
    eyes(art, "#C8E05A", [head(-8, -3)], 1.3, 0.5)
    return art


# ---- Chupacabra -----------------------------------------------------------------------------------
# The Southwest's goat-sucker: lean and hairless, blue-grey skin stretched over its ribs, high and
# hunched at the shoulders with its head carried low, a ridge of spines down its back, a long snout
# with fangs, a bare rat's tail, and red eyes. A slinking walk.

CHUPA = "#5C6268"
CHUPA_FAR = "#44494E"
CHUPA_DETAIL = "#363A3F"
CHUPA_MOON = "#C4CACF"


def chupacabra(phase):
    hind_near, fore_near = phase, phase + math.pi * 0.5
    hind_far, fore_far = phase + math.pi, phase + math.pi * 1.5

    def fore_leg(ph, shoulder):
        a = -22 * math.sin(ph)
        elbow = ray(shoulder, a, 15)
        bend = 6 + 55 * max(0.0, math.sin(ph - math.pi * 0.6)) ** 1.3
        wrist = ray(elbow, a - bend * 0.5, 14)
        toe = ray(wrist, a - bend * 1.5 + 90, 4.5)
        return elbow, wrist, toe

    def hind_leg(ph, hip):
        a = 8 - 20 * math.sin(ph)
        knee = ray(hip, a + 14, 12)
        bend = 30 + 28 * max(0.0, math.sin(ph - math.pi * 0.6)) ** 1.3
        hock = ray(knee, a - bend, 12)
        ankle = ray(hock, a + 4 - bend * 0.15, 8)
        toe = ray(ankle, 90 - 0.6 * bend + a, 4.5)
        return knee, hock, ankle, toe

    rise = 1.0 * math.sin(2 * phase + 0.5)
    pitch = 2.5 + 1.5 * math.sin(2 * phase)
    body = Frame((0, rise), pitch)
    sh, hp = body(-15, 3), body(17, 6)
    legs = {
        "fn": fore_leg(fore_near, sh), "ff": fore_leg(fore_far, sh),
        "hn": hind_leg(hind_near, hp), "hf": hind_leg(hind_far, hp),
    }
    low = lowest([p for l in legs.values() for p in l]) + 1.5
    off = (50.0, GROUND - low)

    def place(p):
        return (p[0] + off[0], p[1] + off[1])

    body = Frame(place((0, rise)), pitch)
    nod = 3 * math.sin(2 * phase - 0.4)
    head = Frame(body(-24, 1), -16 + nod)
    art = Art()

    def fore_parts(l, color):
        e, w, t = (place(p) for p in l)
        s = body(-15, 3)
        art.fill(color, capsule(s, e, 4.8, 2.8) + capsule(e, w, 2.6, 1.8) + capsule(w, t, 1.8, 1.5))
        art.stroke(color, "".join(f"M{f(t[0])},{f(t[1])}L{f(ray(t, 100 + d, 2.4)[0])},{f(ray(t, 100 + d, 2.4)[1])}"
                                  for d in (-20, 0, 20)), 0.6)

    def hind_parts(l, color):
        k, h, a, t = (place(p) for p in l)
        s = body(17, 6)
        art.fill(color, capsule(s, k, 6.0, 3.4) + capsule(k, h, 3.2, 1.9) + capsule(h, a, 1.9, 1.6) + capsule(a, t, 1.7, 1.4))
        art.stroke(color, "".join(f"M{f(t[0])},{f(t[1])}L{f(ray(t, 100 + d, 2.4)[0])},{f(ray(t, 100 + d, 2.4)[1])}"
                                  for d in (-20, 0, 20)), 0.6)
        return k, h

    fore_parts(legs["ff"], CHUPA_FAR)
    hind_parts(legs["hf"], CHUPA_FAR)

    # A bare, thin tail that hangs and sways.
    tail = [body(27, 0)]
    angle = -50.0
    for i in range(8):
        angle += 6 + 6 * math.sin(phase - i * 0.6)
        tail.append(ray(tail[-1], angle, 3.0))
    art.fill(CHUPA, "".join(capsule(tail[i], tail[i + 1], 1.8 - i * 0.18, 1.6 - i * 0.18) for i in range(8)))

    # Deep-chested and pinched at the waist, the shoulders high and the back sloping down.
    torso = shape(body, [
        ("M", -25, 3), ("C", -24, -6, -19, -12, -11, -13), ("C", -3, -13.5, 6, -9, 14, -7),
        ("C", 22, -5, 28, -3, 28.5, 2), ("C", 29, 7, 24, 9, 18, 8.5), ("C", 10, 7, 3, 5, -4, 9),
        ("C", -10, 12, -18, 11, -22, 8.5), ("C", -25, 7, -25.5, 5, -25, 3), ("Z",),
    ])
    skull = shape(head, [
        ("M", 4, -4), ("C", 1, -7, -5, -6.5, -8, -4.5), ("L", -17, -1.5), ("C", -19, -0.8, -19, 1.2, -17, 1.8),
        ("L", -7, 4), ("C", -2, 5, 3, 4, 5, 0.5), ("Z",),
    ])
    ears = shape(head, [("M", 0, -6), ("L", 2, -12.5), ("L", 4, -5.5), ("Z",)])
    art.fill(CHUPA, torso + skull + ears)
    fore_parts(legs["fn"], CHUPA)
    hk, hh = hind_parts(legs["hn"], CHUPA)

    # Spines down the back, longest over the shoulders.
    spines = []
    for x in range(-20, 16, 3):
        t = (x + 20) / 36
        base = body(x, -12.6 + 7 * t * t if x > -11 else -12.6 + (x + 11) ** 2 * 0.02)
        length = 5.5 - 3.5 * t
        tip = ray(base, 170 - 25 * t, length)
        spines.append(poly([(base[0] - 1.1, base[1] + 0.4), tip, (base[0] + 1.1, base[1] + 0.4)]))
    art.fill(INK, "".join(spines))
    # Ribs showing through the skin, the jaw line and fangs.
    art.stroke(CHUPA_DETAIL, "".join(
        shape(body, [("M", x, -6), ("Q", x + 1.5, 0, x - 0.5, 7)]) for x in (-14, -10.5, -7, -3.5)
    ) + shape(head, [("M", -16.5, 0.6), ("L", -7, 1.8)]), 0.7, 0.9)
    art.stroke("#ECE6D8", shape(head, [("M", -15, 1.2), ("L", -14.4, 3.4), ("M", -11, 1.6), ("L", -10.6, 3.2)]), 0.7, 0.9)
    art.stroke(CHUPA_MOON, "".join([
        shape(body, [("M", -23, -1), ("C", -21, -8, -16, -12, -9, -13)]),
        shape(head, [("M", -1, -6.5), ("C", -5, -6.4, -9, -4.5, -14, -2.5)]),
        side_stroke(hk, hh, 3.2, 0.1, 0.9, 1, inset=0.4),
    ]), 0.7, 0.5)
    eyes(art, "#D7262A", [head(-7, -3)], 2.0, 0.8)
    return art


# ---- The Red Ghost -------------------------------------------------------------------------------
# A huge, shaggy red camel, pacing as camels do (both legs on a side together), its long neck bobbing,
# with a dead man strapped upright to its hump: a bare skull, ribs, and arms that swing loose.

RED = "#7A3B26"
RED_FAR = "#5A2B1C"
RED_DETAIL = "#4A2316"
RED_MOON = "#E0B39A"
BONE = "#D9D2BF"
BONE_DARK = "#8F8775"


def red_ghost(phase):
    # A pace: the near fore and near hind swing together, then the far pair.
    near, far = phase, phase + math.pi

    def leg(ph, top, upper, lower, cannon, fore):
        a = -20 * math.sin(ph) + (0 if fore else 4)
        knee = ray(top, a + (0 if fore else 10), upper)
        bend = 6 + 50 * max(0.0, math.sin(ph - math.pi * 0.6)) ** 1.3
        joint = ray(knee, a - (bend * 0.6 if fore else bend * 0.9), lower)
        foot = ray(joint, a + bend * (0.3 if fore else 0.6), cannon)
        toe = ray(foot, 90 - bend * 0.8, 4.0)
        return knee, joint, foot, toe

    rise = 1.2 * math.sin(2 * phase + 0.4)
    roll = 1.5 * math.sin(phase)
    body = Frame((0, rise), roll)
    sh, hp = body(-11, 2), body(11, 2)
    legs = {
        "fn": leg(near, sh, 13, 13, 9, True), "ff": leg(far, sh, 13, 13, 9, True),
        "hn": leg(near, hp, 12, 13, 10, False), "hf": leg(far, hp, 12, 13, 10, False),
    }
    low = lowest([p for l in legs.values() for p in l]) + 1.8
    off = (52.0, GROUND - low)

    def place(p):
        return (p[0] + off[0], p[1] + off[1])

    body = Frame(place((0, rise)), roll)
    art = Art()

    def leg_parts(l, root, color, r):
        k, j, ft, t = (place(p) for p in l)
        s = body(*root)
        art.fill(color, capsule(s, k, r, r * 0.6) + capsule(k, j, r * 0.55, r * 0.45) + capsule(j, ft, r * 0.42, r * 0.38)
                 + capsule(ft, t, r * 0.5, r * 0.45))
        return k, j

    leg_parts(legs["ff"], (-11, 2), RED_FAR, 5.0)
    leg_parts(legs["hf"], (11, 2), RED_FAR, 5.6)

    # The neck curves down from the shoulders and up again to the head, bobbing with the pace.
    bob = 3 * math.sin(2 * phase - 0.8)
    neck = [body(-13, -2)]
    for i, a in enumerate((88, 112, 145, 172, 186)):
        neck.append(ray(neck[-1], a + bob * (i / 4), 5.4))
    nr = [6.0, 5.0, 4.2, 3.6, 3.2, 3.0]
    head = Frame(neck[-1], -8 + bob)
    skull = shape(head, [
        ("M", 3, -3), ("C", 0, -5, -5, -5, -8, -3.5), ("L", -12, -2), ("C", -14, -1, -14, 1.5, -12, 2.5),
        ("L", -4, 3), ("C", 0, 3, 3, 1.5, 3, -3), ("Z",),
    ])
    ear = shape(head, [("M", 1, -4), ("L", 3, -7.5), ("L", 3.5, -3), ("Z",)])
    torso = shape(body, [
        ("M", -16, 0), ("C", -16, -6, -12, -8, -6, -8.5), ("C", 0, -9, 10, -8.5, 15, -6),
        ("C", 19, -4, 19, 3, 16, 6), ("C", 8, 8.5, -6, 8.5, -13, 6), ("C", -16, 5, -16.5, 3, -16, 0), ("Z",),
    ])
    hump = shape(body, [("M", -8, -7.5), ("C", -6, -17, 6, -18, 9, -7.5), ("Z",)])
    tail = capsule(body(17, -2), ray(body(17, -2), -20 + 8 * math.sin(phase), 9), 1.3, 0.9)
    art.fill(RED, torso + hump + tail + "".join(capsule(neck[i], neck[i + 1], nr[i], nr[i + 1]) for i in range(5)) + skull + ear)

    # The rider: strapped upright to the hump, slumped a little, swaying with the pace. A skull, a
    # ribcage, and arms hanging loose that swing a beat behind.
    seat = body(0.5, -15.5)
    sway = 4 * math.sin(phase + 0.5)
    rider = Frame(seat, -6 + sway)
    spine_top = rider(0, -16)
    skull_c = rider(-0.5, -20)
    art.fill(BONE_DARK, capsule(rider(-1, -15), ray(rider(-1, -15), 4 + sway * 1.5, 13), 0.9, 0.7)
             + capsule(ray(rider(-1, -15), 4 + sway * 1.5, 13), ray(ray(rider(-1, -15), 4 + sway * 1.5, 13), -10 + sway, 9), 0.7, 0.6))
    art.fill(BONE, capsule(rider(0, 0), spine_top, 1.0, 0.8) + circle(skull_c, 3.0)
             + capsule(rider(1.5, 2), ray(rider(1.5, 2), 30, 6), 1.1, 0.9))
    ribs = "".join(shape(rider, [("M", -3.2, -14 + k * 2.4), ("Q", 0, -12.5 + k * 2.4, 3.2, -14 + k * 2.4)]) for k in range(5))
    art.stroke(BONE, ribs, 0.8)
    art.fill(INK, circle(rider(-1.6, -20.4), 0.8) + circle(rider(0.6, -20.4), 0.8) + shape(rider, [("M", -1.5, -18.2), ("L", 0.8, -18.2), ("L", -0.3, -17.4), ("Z",)]))
    # The near arm hangs and swings, and rawhide straps hold the body to the hump.
    arm_top = rider(-0.5, -14.5)
    elbow = ray(arm_top, -2 + sway * 2, 7)
    hand = ray(elbow, -8 + sway * 1.2, 7)
    art.fill(BONE, capsule(arm_top, elbow, 0.9, 0.7) + capsule(elbow, hand, 0.7, 0.6))
    art.stroke("#2A1A10", shape(body, [("M", -7, -12), ("Q", 0.5, -7, 8, -12)]) + shape(rider, [("M", -3.5, -9), ("L", 3.5, -9)])
               + shape(rider, [("M", -3.5, -4), ("L", 3.5, -4)]), 1.2, 0.95)

    leg_parts(legs["fn"], (-11, 2), RED, 5.0)
    k, j = leg_parts(legs["hn"], (11, 2), RED, 5.6)
    # Shaggy hair on the hump, the neck and the tops of the legs.
    art.stroke(RED_DETAIL, tufts(neck[0], neck[2], 5.5, 4, side=-1, length=2.0)
               + shape(body, [("M", -5, -12), ("L", -6, -9), ("M", -1, -14), ("L", -2, -11), ("M", 3, -14), ("L", 2.5, -11), ("M", 7, -11), ("L", 6.5, -8.5)])
               + shape(body, [("M", -10, -3), ("Q", -8, 2, -10, 6)]) + shape(body, [("M", 9, -4), ("Q", 11, 1, 9, 6)]), 0.8, 0.9)
    art.stroke(RED_MOON, "".join([
        shape(body, [("M", -7.5, -9), ("C", -5.5, -16, 3, -17.5, 7, -12)]),
        shape(head, [("M", 1, -4.2), ("C", -3, -5, -7, -4.5, -10, -3)]),
        side_stroke(k, j, 3.1, 0.1, 0.9, 1, inset=0.4),
    ]), 0.8, 0.5)
    eyes(art, "#F2C14E", [head(-5.5, -2)], 1.4, 0.55)
    return art


# ---- Dark Watchers -------------------------------------------------------------------------------
# A tall, dark figure standing perfectly still on the skyline, facing you: a wide-brimmed hat with
# nothing to see under it, narrow shoulders and a long cloak down to the ankles. It never moves.
# Only the wind does, stirring the cloak's edges and hem. Its frames loop on the clock, not a stride.

def dark_watcher(phase):
    art = Art()

    def flutter(y, side):
        # The wind blows from the right, so that edge streams out and ripples more. The ripple
        # grows towards the hem and runs downward.
        reach = ((y - 24) / 66) ** 1.5
        gust = 1.2 if side > 0 else 0.5
        return side * reach * gust * (1.2 + 1.0 * math.sin(phase - y * 0.12))

    # The cloak: from the shoulders it falls straight, then flares a little to the hem.
    left, right = [], []
    for k in range(14):
        y = 24 + 66 * k / 13
        half = 8.5 + 5.5 * ((y - 24) / 66) ** 1.3
        left.append((50 - half + flutter(y, -1), y))
        right.append((50 + half + flutter(y, 1), y))
    hem = []
    for k in range(1, 8):
        x = left[-1][0] + (right[-1][0] - left[-1][0]) * k / 8
        hem.append((x, 90 + 0.9 * math.sin(phase * 2 + k * 1.3) - 0.6 * (k % 2)))
    cloak = poly([(46, 22.5), (54, 22.5)] + right + list(reversed(hem)) + list(reversed(left)))
    # Two thin legs below the hem.
    legs = capsule((47, 88), (46.6, 94), 1.5, 1.2) + capsule((53, 88), (53.4, 94), 1.5, 1.2)
    feet = poly([(44.4, 93.4), (48, 93.4), (48, 95), (44, 95)]) + poly([(52, 93.4), (55.6, 93.4), (56, 95), (52, 95)])
    art.fill(FAR, legs + feet)
    head = circle((50, 18.5), 4.2)
    crown = path([("M", 44.5, 13.5), ("C", 44.5, 9, 45.5, 6.5, 50, 6.5), ("C", 54.5, 6.5, 55.5, 9, 55.5, 13.5), ("Z",)])
    brim = path([("M", 36, 14.8), ("C", 40, 12.4, 60, 12.4, 64, 14.8), ("C", 60, 16.4, 40, 16.4, 36, 14.8), ("Z",)])
    art.fill(INK, cloak + head + crown + brim)
    # A band round the crown, and the cloak's folds, which sway with it.
    art.stroke(DETAIL, path([("M", 44.6, 12.2), ("Q", 50, 13.2, 55.4, 12.2)]), 0.9, 0.9)
    folds = ""
    for x0 in (45.5, 50, 54.5):
        pts = [(x0 + (x0 - 50) * 0.35 * (y - 30) / 58 + flutter(y, 1 if x0 > 50 else -1) * 0.4, y) for y in range(30, 89, 6)]
        folds += "M" + "L".join(f"{f(x)},{f(y)}" for x, y in pts)
    art.stroke(DETAIL, folds, 0.8, 0.8)
    art.stroke(MOON, path([("M", 37, 14.6), ("C", 40, 13, 44, 12.8, 46, 12.8), ("M", 45, 9), ("C", 45.6, 7.4, 47, 6.8, 49, 6.7)])
               + "M" + "L".join(f"{f(x + 0.7)},{f(y)}" for x, y in left[:9]), 0.8, 0.45)
    return art


# ---- Fliers -------------------------------------------------------------------------------------
# Seen from the front, wings spread either side. Each wing is drawn flat in its own coordinates, x
# out from the shoulder and y down from the leading edge, then bent into the pose for the moment.


def smooth(e0, e1, x):
    t = min(1.0, max(0.0, (x - e0) / (e1 - e0)))
    return t * t * (3 - 2 * t)


class Wing:
    """
    One wing in one pose. `side` is 1 for the wing on the right of the picture, -1 for the left.
    The angle above level at a distance x out along the wing comes from `angle(x)`, so a wing can
    bend: the flap runs out from the shoulder, and the tip trails it. `chord` squashes the wing's
    depth, as it turns edge-on on the upstroke.
    """

    STEP = 0.5

    def __init__(self, shoulder, side, angle, chord=1.0):
        self.shoulder, self.side, self.angle, self.chord = shoulder, side, angle, chord
        self.spine = [(shoulder[0], shoulder[1])]

    def _along(self, x):
        while (len(self.spine) - 1) * self.STEP < x:
            n = len(self.spine) - 1
            a = math.radians(self.angle((n + 0.5) * self.STEP))
            px, py = self.spine[-1]
            self.spine.append((px + self.side * math.cos(a) * self.STEP, py - math.sin(a) * self.STEP))
        i = x / self.STEP
        lo = int(i)
        hi = min(lo + 1, len(self.spine) - 1)
        t = i - lo
        (ax, ay), (bx, by) = self.spine[lo], self.spine[hi]
        return ax + (bx - ax) * t, ay + (by - ay) * t

    def __call__(self, x, y):
        x = max(0.0, x)
        px, py = self._along(x)
        a = math.radians(self.angle(x))
        return px + self.side * math.sin(a) * y * self.chord, py + math.cos(a) * y * self.chord

    def poly(self, pts):
        q = [self(x, y) for x, y in pts]
        return "M" + "L".join(f"{f(a)},{f(b)}" for a, b in q) + "Z"

    def line(self, pts, samples=6):
        """A stroke through `pts`, sampled finely so it bends with the wing."""
        out = []
        for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
            for k in range(samples + (1 if (x1, y1) == pts[-1] else 0)):
                t = k / samples
                out.append(self(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t))
        return "M" + "L".join(f"{f(a)},{f(b)}" for a, b in out)


def wingbeat(phase, base, amp, length, lag, droop=0.0):
    """The angle along a wing: a beat that runs from shoulder to tip, lagging `lag` radians at the tip."""
    return lambda x: base + amp * math.sin(phase - lag * x / length) - droop * (x / length) ** 2


def pair(shoulder_l, shoulder_r, angle, chord):
    return Wing(shoulder_l, -1, angle, chord), Wing(shoulder_r, 1, angle, chord)


def poly(pts, dx=0.0, dy=0.0):
    return "M" + "L".join(f"{f(x + dx)},{f(y + dy)}" for x, y in pts) + "Z"


def path(cmds, dx=0.0, dy=0.0):
    return shape(Frame((dx, dy)), cmds)


def mirror_x(cmds, about=50.0):
    out = []
    for c in cmds:
        op, pts = c[0], list(c[1:])
        for i in range(0, len(pts), 2):
            pts[i] = 2 * about - pts[i]
        out.append((op, *pts))
    return out


def dangle(top, angle, l1, l2, bend):
    """A leg hanging from `top`: knee then ankle, bending back by `bend` degrees at the knee."""
    knee = ray(top, angle, l1)
    return knee, ray(knee, angle - bend, l2)


# ---- Mothman --------------------------------------------------------------------------------------
# A man-shaped body under huge moth wings, red eyes, legs hanging straight. Slow, heavy beats.

MOTH_WING = [(0, -1.5), (10, -3.5), (22, -4.5), (33, -4), (39, -1.5), (37, 4), (33, 9),
             (37, 13), (39, 18), (33, 23), (21, 24.5), (11, 21.5), (4, 16), (0, 11)]


def mothman(phase):
    bob = 3.2 * math.sin(phase - 0.5)
    angle = wingbeat(phase, 16, 34, 40, 1.1, droop=6)
    chord = 0.84 - 0.16 * math.cos(phase)
    wl, wr = pair((43, 31 + bob), (57, 31 + bob), angle, chord)
    art = Art()
    art.fill(INK, wl.poly(MOTH_WING) + wr.poly(MOTH_WING))
    # Veins, and the eye-spots that stare back from each wing.
    veins = [[(2, 1), (20, -1.5), (37, -1)], [(3, 5), (20, 4), (34, 7.5)], [(3, 9), (20, 11), (37, 16)],
             [(2, 13), (16, 18), (27, 22.5)]]
    art.stroke(DETAIL, "".join(w.line(v) for w in (wl, wr) for v in veins), 0.8, 0.9)
    art.stroke(DETAIL, "".join(circle(w(23, 6), 3.2) + circle(w(23, 6), 1.2) for w in (wl, wr)), 0.9, 0.9)
    art.stroke(MOON, wl.line([(1, -1.5), (12, -3.6), (24, -4.5), (34, -4), (38.5, -1.6)]), 0.8, 0.45)
    art.stroke(MOON, wr.line([(1, -1.5), (12, -3.6), (24, -4.5), (34, -4)]), 0.7, 0.25)

    # The legs swing a beat behind the body, splaying a little as it rises.
    swing = 4 * math.sin(phase - 1.6)
    spread = 2 + 1.5 * math.sin(phase - 2.0)
    lk_l, la_l = dangle((46, 60 + bob), -spread + swing * 0.4, 15, 15, 3 + swing)
    lk_r, la_r = dangle((54, 60 + bob), spread + swing * 0.4, 15, 15, 3 + swing)
    for hip, k, a in (((46, 60 + bob), lk_l, la_l), ((54, 60 + bob), lk_r, la_r)):
        art.fill(INK, capsule(hip, k, 4.2, 3.2) + capsule(k, a, 3.2, 2.0))
    body = [("M", 50, 13), ("C", 54.5, 13, 56.5, 16.5, 56, 22), ("L", 57.5, 29), ("C", 60, 33, 60.5, 44, 59, 52),
            ("C", 58.5, 57, 59, 61, 58, 64), ("L", 42, 64), ("C", 41, 61, 41.5, 57, 41, 52),
            ("C", 39.5, 44, 40, 33, 42.5, 29), ("L", 44, 22), ("C", 43.5, 16.5, 45.5, 13, 50, 13), ("Z",)]
    art.fill(INK, path(body, 0, bob))
    art.stroke(DETAIL, path([("M", 46.5, 30), ("L", 48, 33.5), ("M", 50, 31), ("L", 50, 34.5), ("M", 53.5, 30),
                             ("L", 52, 33.5), ("M", 44, 40), ("Q", 50, 43, 56, 40), ("M", 44.5, 48),
                             ("Q", 50, 50.5, 55.5, 48)], 0, bob), 0.8, 0.9)
    art.stroke(MOON, path([("M", 45, 16), ("C", 46, 14, 48, 13, 50, 13), ("M", 42.6, 30), ("C", 40.5, 36, 40.4, 44, 41.4, 51)], 0, bob)
               + segment((46 - 3.6, 61 + bob), lk_l, 0.1, 0.9), 0.8, 0.45)
    eyes(art, "#D7262A", [(46.5, 20.5 + bob), (53.5, 20.5 + bob)], 4, 2)
    return art


# ---- Jersey Devil -----------------------------------------------------------------------------------
# A horse's head with horns, bat wings beating fast, hooves kicking, and a forked tail lashing about.

BAT_WING = [(0, -1.5), (9, -3.5), (17, -5.5), (28, -4), (39, -2.5), (33.5, 3), (34, 10.5), (28.5, 10),
            (24, 16.5), (17.5, 12), (10, 15.5), (5, 12.5), (0, 11)]
BAT_FINGERS = [[(1, -1), (17, -4.5)], [(17, -4.5), (38, -2.5)], [(17, -4.5), (33.5, 9.5)], [(17, -4.5), (24, 15)]]


def jersey_devil(phase):
    bob = 3.6 * math.sin(phase - 0.6)
    angle = wingbeat(phase, 18, 40, 39, 0.9, droop=8)
    chord = 0.82 - 0.18 * math.cos(phase)
    wl, wr = pair((45, 40 + bob), (55, 40 + bob), angle, chord)
    art = Art()

    # The tail hangs behind the body and lashes side to side, the wave running down to the fork.
    tail = [(52, 66 + bob)]
    a = -30.0
    for i in range(8):
        a += -5 + 9 * math.sin(phase - i * 0.55)
        tail.append(ray(tail[-1], a, 3.3))
    radii = [2.4 - i * 0.2 for i in range(9)]
    art.fill(FAR, "".join(capsule(tail[i], tail[i + 1], radii[i], radii[i + 1]) for i in range(8)))
    tip, prev = tail[-1], tail[-2]
    da = math.degrees(math.atan2(-(tip[0] - prev[0]), tip[1] - prev[1]))
    fork_l, fork_r = ray(tip, da + 35, 5), ray(tip, da - 35, 5)
    art.fill(FAR, poly([ray(tip, da + 90, 1.8), fork_l, ray(tip, da, 2), fork_r, ray(tip, da - 90, 1.8)]))

    art.fill(INK, wl.poly(BAT_WING) + wr.poly(BAT_WING))
    art.stroke(DETAIL, "".join(w.line(b) for w in (wl, wr) for b in BAT_FINGERS), 0.9, 0.9)
    # A hooked thumb-claw at each wrist.
    art.stroke(INK, "".join(w.line([(17, -4.5), (16, -8), (14.5, -9)], 3) for w in (wl, wr)), 1.1)
    art.stroke(MOON, wl.line([(1, -1.6), (9, -3.6), (17, -5.6), (28, -4.1), (38.5, -2.6)]), 0.8, 0.45)
    art.stroke(MOON, wr.line([(1, -1.6), (9, -3.6), (17, -5.6), (28, -4.1)]), 0.7, 0.25)

    # Hind legs tuck up on the downstroke and kick down after it, hooves dangling.
    tuck = 0.5 + 0.5 * math.sin(phase - 2.2)
    for side in (-1, 1):
        hip = (50 + side * 4.5, 64 + bob)
        knee = ray(hip, -side * (8 + 6 * tuck), 12 - 3 * tuck)
        hock = ray(knee, side * (10 + 4 * tuck), 10 - 4 * tuck)
        hoof = ray(hock, side * 3, 6)
        art.fill(INK, capsule(hip, knee, 4.0, 2.8) + capsule(knee, hock, 2.6, 1.8) + capsule(hock, hoof, 1.8, 1.6))
        art.fill(INK, poly([(hoof[0] - 2.4, hoof[1] - 0.5), (hoof[0] + 2.4, hoof[1] - 0.5), (hoof[0] + 2, hoof[1] + 2.4), (hoof[0] - 2, hoof[1] + 2.4)]))
    body = [("M", 47, 30), ("L", 53, 30), ("L", 54, 36), ("C", 58, 38, 59, 46, 57.5, 54), ("C", 57, 60, 56.5, 64, 55, 67),
            ("L", 45, 67), ("C", 43.5, 64, 43, 60, 42.5, 54), ("C", 41, 46, 42, 38, 46, 36), ("Z",)]
    head = [("M", 50, 14), ("C", 53, 14, 55, 17, 55, 21), ("C", 55, 25, 53.5, 28, 53, 31), ("C", 52.6, 34, 51.5, 35, 50, 35),
            ("C", 48.5, 35, 47.4, 34, 47, 31), ("C", 46.5, 28, 45, 25, 45, 21), ("C", 45, 17, 47, 14, 50, 14), ("Z",)]
    horn = [("M", 46.5, 16.5), ("C", 44, 13, 42, 11, 39.5, 10.5), ("C", 42, 13, 43.5, 16, 45, 18.5), ("Z",)]
    ear = [("M", 46, 18), ("L", 42.5, 17.5), ("L", 45.5, 21), ("Z",)]
    art.fill(INK, path(body, 0, bob) + path(head, 0, bob) + path(horn, 0, bob) + path(mirror_x(horn), 0, bob)
             + path(ear, 0, bob) + path(mirror_x(ear), 0, bob))
    # Little forelimbs with claws, held up against the chest.
    claw = 2 * math.sin(phase - 1.0)
    for side in (-1, 1):
        sh = (50 + side * 5.5, 41 + bob)
        el = (50 + side * 7.5, 48 + bob)
        wr_ = (50 + side * 4.5, 50 + bob + claw * 0.4)
        art.fill(INK, capsule(sh, el, 2.4, 1.8) + capsule(el, wr_, 1.8, 1.3))
        art.stroke(INK, "".join(f"M{f(wr_[0])},{f(wr_[1])}L{f(wr_[0] - side * k)},{f(wr_[1] + 2.6)}" for k in (0.4, 1.4, 2.4)), 0.6)
    art.stroke(DETAIL, path([("M", 46.5, 44), ("Q", 50, 46, 53.5, 44), ("M", 46.5, 50), ("Q", 50, 52, 53.5, 50),
                             ("M", 46.8, 56), ("Q", 50, 58, 53.2, 56), ("M", 48, 31.5), ("Q", 50, 33, 52, 31.5),
                             ("M", 48.6, 33.4), ("L", 48.6, 34), ("M", 51.4, 33.4), ("L", 51.4, 34)], 0, bob), 0.8, 0.9)
    art.stroke(MOON, path([("M", 45.4, 24), ("C", 45, 20, 46.5, 15, 50, 14.2), ("M", 45.5, 36.5), ("C", 42.5, 40, 41.6, 47, 42.8, 54),
                           ("M", 46, 16.6), ("C", 44, 13.6, 42, 11.6, 40, 11)], 0, bob), 0.8, 0.45)
    eyes(art, "#D7262A", [(47.6, 22 + bob), (52.4, 22 + bob)], 2, 0.8)
    return art


# ---- The winged monster -----------------------------------------------------------------------------
# The Southwest's featherless flier, seen from the front: long, narrow wings of bare skin running out
# to sharp tips, a hooked claw at each wrist, a small body, a crested head with a long beak hanging
# down towards you, and clawed legs trailing below. Slow, heavy beats with long glides between.

PTERO_WING = [(0, -2), (12, -4), (19, -6), (32, -4.5), (48, -1.5), (38, 3), (26, 7), (13, 11), (4, 14), (0, 13)]
PTERO_SKIN = "#2E2925"
PTERO_BODY = "#3E3833"


def winged_monster(phase):
    bob = 4.0 * math.sin(phase - 0.6)
    # Mostly gliding: the beat is strongest in one part of the cycle and the wings hold level after.
    beat = math.sin(phase) * (0.6 + 0.4 * math.sin(phase))
    angle = lambda x: 6 + 30 * beat - 10 * (x / 48) ** 2 + 6 * math.sin(phase - 1.0 * x / 48)
    chord = 0.85 - 0.15 * math.cos(phase)
    wl, wr = pair((45, 42 + bob), (55, 42 + bob), angle, chord)
    art = Art()
    art.fill(PTERO_SKIN, wl.poly(PTERO_WING) + wr.poly(PTERO_WING))
    # The long wing finger along the leading edge, and creases in the membrane.
    art.stroke(DETAIL, "".join(w.line(c) for w in (wl, wr) for c in (
        [(1, -1.5), (19, -5.5), (47, -1.5)], [(19, -5.5), (26, 6.5)], [(19, -5.5), (12, 10)])), 0.8, 0.9)
    art.stroke(INK, "".join(w.line([(19, -6), (18.5, -9.5), (16.5, -10.5)], 3) for w in (wl, wr)), 1.1)
    art.stroke(MOON, wl.line([(1, -2), (12, -4.1), (19, -6.1), (32, -4.6), (47.5, -1.6)]), 0.8, 0.45)
    art.stroke(MOON, wr.line([(1, -2), (12, -4.1), (19, -6.1), (32, -4.6)]), 0.7, 0.25)

    # Legs trailing below, claws curled, swinging a little behind the beat.
    swing = 3 * math.sin(phase - 1.8)
    for side in (-1, 1):
        hip = (50 + side * 3, 56 + bob)
        knee, ankle = dangle(hip, side * 6 + swing * 0.4, 9, 9, 4 + swing)
        art.fill(PTERO_BODY, capsule(hip, knee, 2.4, 1.7) + capsule(knee, ankle, 1.7, 1.2))
        art.stroke(INK, "".join(f"M{f(ankle[0])},{f(ankle[1])}Q{f(ankle[0] + k)},{f(ankle[1] + 3)} {f(ankle[0] + k * 1.6)},{f(ankle[1] + 2.6)}"
                                for k in (-1.4, 0, 1.4)), 0.7)
    body = [("M", 50, 34), ("C", 53.5, 34, 55.5, 38, 55, 44), ("C", 54.5, 50, 53, 56, 50, 58), ("C", 47, 56, 45.5, 50, 45, 44),
            ("C", 44.5, 38, 46.5, 34, 50, 34), ("Z",)]
    # The head, seen from the front: a narrow skull with a crest rising behind it, and the long
    # beak pointing down and out towards the viewer.
    head = [("M", 50, 22), ("C", 53, 22, 54.5, 25, 54, 29), ("C", 53.5, 32, 52, 34, 50, 34.5), ("C", 48, 34, 46.5, 32, 46, 29),
            ("C", 45.5, 25, 47, 22, 50, 22), ("Z",)]
    crest = [("M", 48.5, 23), ("C", 48, 17, 49, 11, 51.5, 7), ("C", 51.5, 12, 52, 18, 52, 23), ("Z",)]
    beak = [("M", 48.3, 31), ("L", 51.7, 31), ("L", 50.4, 44), ("L", 49.6, 44), ("Z",)]
    art.fill(PTERO_BODY, path(body, 0, bob) + path(head, 0, bob) + path(crest, 0, bob))
    art.fill(INK, path(beak, 0, bob))
    art.stroke(DETAIL, path([("M", 47, 40), ("Q", 50, 42, 53, 40), ("M", 47.2, 46), ("Q", 50, 48, 52.8, 46),
                             ("M", 49.2, 33), ("L", 49.8, 43)], 0, bob), 0.7, 0.9)
    art.stroke(MOON, path([("M", 46.5, 27), ("C", 46.4, 24.5, 47.6, 22.4, 50, 22.2), ("M", 49, 21), ("C", 49.2, 15, 50, 10, 51.4, 7.6),
                           ("M", 45.2, 40), ("C", 45, 46, 46.5, 52, 49, 56.5)], 0, bob), 0.8, 0.45)
    eyes(art, "#F2C14E", [(47.8, 27.5 + bob), (52.2, 27.5 + bob)], 1.6, 0.6)
    return art


# ---- Thunderbird -----------------------------------------------------------------------------------
# An enormous bird with a hooked beak, the long primaries spread like fingers. Big slow beats.

BIRD_WING = [(0, -2), (12, -4.5), (24, -4.5), (31, -3), (31, 12), (27, 12), (24, 15.5), (21, 13), (18, 16),
             (15, 13.5), (12, 16), (9, 13.5), (6, 15), (3, 13), (0, 12)]
PRIMARIES = [(27, -3, 44, -1.5, 2.4), (28, 0, 43, 3.5, 2.4), (29, 3.5, 41.5, 8, 2.3), (29, 7, 39, 12, 2.2), (29, 10, 36, 15, 2.1)]


def thunderbird(phase):
    bob = 4.5 * math.sin(phase - 0.5)
    angle = wingbeat(phase, 10, 30, 47, 1.0, droop=10)
    # On the upstroke the primaries open into a gap-fingered fan, and the wing turns edge-on.
    chord = 0.8 - 0.2 * math.cos(phase)
    spread = 0.5 + 0.5 * math.sin(phase + 0.4)
    wl, wr = pair((44, 41 + bob), (56, 41 + bob), angle, chord)
    art = Art()

    # The tail fans out under the body, tilting a touch as the bird steers.
    tilt = 3 * math.sin(phase * 0.5)
    tail = [("M", 46, 56), ("L", 39, 72 + tilt), ("L", 43, 74 + tilt * 0.5), ("L", 47, 73), ("L", 50, 76),
            ("L", 53, 73), ("L", 57, 74 - tilt * 0.5), ("L", 61, 72 - tilt), ("L", 54, 56), ("Z",)]
    art.fill(INK, path(tail, 0, bob))
    for w in (wl, wr):
        art.fill(INK, w.poly(BIRD_WING))
        for x0, y0, x1, y1, r in PRIMARIES:
            # Splayed further apart as the wing opens, closed into a blade on the downstroke.
            ys = (y1 - y0 * 0.2) * (0.75 + 0.35 * spread)
            mid = w(x0 + (x1 - x0) * 0.5, y0 + (ys + y0 * 0.2 - y0) * 0.5)
            art.fill(INK, capsule(w(x0, y0), mid, r, r * 0.85) + capsule(mid, w(x1, y0 * 0.2 + ys), r * 0.85, r * 0.45))
    coverts = [[(2, 3), (14, 1), (27, 2)], [(2, 7), (14, 6), (28, 7)]]
    art.stroke(DETAIL, "".join(w.line(c) for w in (wl, wr) for c in coverts), 0.8, 0.9)
    art.stroke(MOON, wl.line([(1, -2), (12, -4.6), (24, -4.6), (30, -3.2)]), 0.8, 0.45)
    art.stroke(MOON, wr.line([(1, -2), (12, -4.6), (24, -4.6), (30, -3.2)]), 0.7, 0.25)

    body = [("M", 50, 36), ("C", 55, 36, 58, 40, 58, 46), ("C", 58, 52, 55.5, 58, 50, 59), ("C", 44.5, 58, 42, 52, 42, 46),
            ("C", 42, 40, 45, 36, 50, 36), ("Z",)]
    head = [("M", 50, 27), ("C", 54, 27, 56, 30, 55.5, 34), ("L", 54, 38), ("L", 46, 38), ("L", 44.5, 34.5),
            ("L", 41, 34), ("C", 39.5, 34.5, 39, 36.5, 40, 37.5), ("C", 40.5, 36.6, 41.4, 36.4, 42.4, 36.7),
            ("L", 44.6, 36.4), ("L", 44.4, 33), ("C", 44.4, 29.5, 46.5, 27, 50, 27), ("Z",)]
    art.fill(INK, path(body, 0, bob) + path(head, 0, bob))
    # Talons drawn up under the body, flexing with each beat.
    grip = 0.5 + 0.5 * math.sin(phase - 1.8)
    talons = ""
    for side in (-1, 1):
        x = 50 + side * 3.5
        talons += f"M{f(x)},{f(57 + bob)}L{f(x)},{f(61 + bob)}"
        for k in (-1.6, 0, 1.6):
            talons += f"M{f(x)},{f(61 + bob)}Q{f(x + k * 1.2)},{f(63 + bob)} {f(x + k * (0.6 + 0.6 * grip))},{f(64 + bob - grip)}"
    art.stroke(INK, talons, 1.0)
    art.stroke(DETAIL, path([("M", 45, 40), ("Q", 50, 43, 55, 40), ("M", 45.5, 45), ("L", 47, 47.5), ("M", 50, 45.5),
                             ("L", 50, 48), ("M", 54.5, 45), ("L", 53, 47.5), ("M", 46.5, 51), ("L", 48, 53), ("M", 53.5, 51),
                             ("L", 52, 53), ("M", 47, 62), ("L", 45, 70), ("M", 50, 62), ("L", 50, 72), ("M", 53, 62), ("L", 55, 70)], 0, bob), 0.8, 0.9)
    art.stroke(MOON, path([("M", 45, 31), ("C", 46, 28.5, 48, 27, 50, 27), ("M", 42.6, 43), ("C", 42.4, 48, 43.5, 53, 46, 56.5)], 0, bob), 0.8, 0.45)
    eyes(art, "#F2C14E", [(47, 32 + bob)], 2, 0.7)
    return art


# ---- Black panther -------------------------------------------------------------------------------
# A long, low black cat, padding along with its head level and its heavy tail swinging low, then
# curling up at the tip. Green eyeshine. It has no spines, no ribs showing: all smooth muscle.

PANTHER_FAR = "#1C1A18"
PANTHER_DETAIL = "#2E2B27"
PANTHER_MOON = "#9DA3A8"


def black_panther(phase):
    hind_near, fore_near = phase, phase + math.pi * 0.5
    hind_far, fore_far = phase + math.pi, phase + math.pi * 1.5

    def fore_leg(ph, shoulder):
        a = -20 * math.sin(ph)
        elbow = ray(shoulder, a, 12)
        bend = 4 + 60 * max(0.0, math.sin(ph - math.pi * 0.6)) ** 1.3
        wrist = ray(elbow, a - bend * 0.5, 11)
        paw = ray(wrist, a - bend * 1.3 + 90, 3.6)
        return elbow, wrist, paw

    def hind_leg(ph, hip):
        a = 8 - 19 * math.sin(ph)
        knee = ray(hip, a + 16, 11)
        bend = 32 + 26 * max(0.0, math.sin(ph - math.pi * 0.6)) ** 1.3
        hock = ray(knee, a - bend, 11)
        ankle = ray(hock, a + 6 - bend * 0.15, 6)
        paw = ray(ankle, 90 - 0.6 * bend + a, 3.6)
        return knee, hock, ankle, paw

    rise = 0.6 * math.sin(2 * phase + 0.5)
    body = Frame((0, rise), 0.5 * math.sin(2 * phase))
    sh, hp = body(-17, 2), body(15, 2)
    legs = {
        "fn": fore_leg(fore_near, sh), "ff": fore_leg(fore_far, sh),
        "hn": hind_leg(hind_near, hp), "hf": hind_leg(hind_far, hp),
    }
    low = lowest([p for l in legs.values() for p in l]) + 1.6
    off = (46.0, GROUND - low)

    def place(p):
        return (p[0] + off[0], p[1] + off[1])

    body = Frame(place((0, rise)), 0.5 * math.sin(2 * phase))
    head = Frame(body(-24, -4), -4 + 2 * math.sin(2 * phase - 0.4))
    art = Art()

    def paw(t, color):
        art.fill(color, path([("M", t[0] - 2.6, t[1] + 1.4), ("C", t[0] - 2.6, t[1] - 1.2, t[0] + 2.2, t[1] - 1.2, t[0] + 2.2, t[1] + 1.4), ("Z",)]))

    def fore_parts(l, color):
        e, w, t = (place(p) for p in l)
        s = body(-17, 2)
        art.fill(color, capsule(s, e, 5.0, 3.0) + capsule(e, w, 2.8, 2.1) + capsule(w, t, 2.1, 1.9))
        paw(t, color)
        return e, w

    def hind_parts(l, color):
        k, h, a, t = (place(p) for p in l)
        s = body(15, 2)
        art.fill(color, capsule(s, k, 6.5, 3.6) + capsule(k, h, 3.2, 2.0) + capsule(h, a, 2.0, 1.8) + capsule(a, t, 1.8, 1.7))
        paw(t, color)
        return k, h

    fore_parts(legs["ff"], PANTHER_FAR)
    hind_parts(legs["hf"], PANTHER_FAR)

    # The tail hangs low from the rump, nearly to the ground, then the last part curls up.
    tail = [body(23, -4)]
    angle = -38.0
    for i in range(12):
        angle += (2.0 if i < 7 else -17) + 3 * math.sin(phase - i * 0.5)
        tail.append(ray(tail[-1], angle, 2.7))
    art.fill(INK, "".join(capsule(tail[i], tail[i + 1], 2.4 - i * 0.07, 2.3 - i * 0.07) for i in range(12)))

    # A long, level back, deep chest and a tucked-up belly.
    torso = shape(body, [
        ("M", -24, -2), ("C", -23, -9, -17, -11, -9, -10.5), ("C", 0, -10, 10, -11, 18, -9.5),
        ("C", 24, -8, 26, -3, 24, 2), ("C", 22, 6, 17, 7, 13, 5.5), ("C", 7, 3.5, 0, 3.5, -6, 6),
        ("C", -12, 8.5, -19, 8, -22, 5), ("C", -24, 3, -24.5, 1, -24, -2), ("Z",),
    ])
    skull = shape(head, [
        ("M", 5, -2), ("C", 4, -6.5, -1, -8, -5, -7.5), ("C", -8.5, -7, -10.5, -5, -11.2, -2.5),
        ("C", -11.8, -0.5, -11, 1.8, -8.5, 2.6), ("C", -4, 4, 2, 3.5, 5, -2), ("Z",),
    ])
    ears = shape(head, [("M", -1.5, -7.2), ("C", -1.5, -10.5, 1.8, -10.5, 2.3, -6.6), ("Z",)])
    art.fill(INK, torso + skull + ears)
    fe, fw = fore_parts(legs["fn"], INK)
    hk, hh = hind_parts(legs["hn"], INK)

    # Muscle along the shoulder and haunch, the mouth line and whiskers.
    art.stroke(PANTHER_DETAIL, "".join([
        shape(body, [("M", -18, -4), ("C", -14, -1, -13, 3, -15, 6)]),
        shape(body, [("M", 11, -6), ("C", 17, -4, 19, 0, 16, 4)]),
        shape(head, [("M", -10.5, 0.8), ("L", -7, 1.2)]),
    ]), 0.7, 0.9)
    art.stroke(PANTHER_MOON, "".join([
        shape(body, [("M", -20, -8.5), ("C", -14, -10.5, -4, -10.2, 6, -10.6)]),
        shape(head, [("M", -1, -7.6), ("C", -5, -7.6, -9, -6, -10.8, -3)]),
        side_stroke(hk, hh, 3.2, 0.1, 0.9, 1, inset=0.4),
        side_stroke(fe, fw, 2.8, 0.1, 0.9, 1, inset=0.4),
    ]), 0.6, 0.4)
    art.stroke("#C9C4B6", shape(head, [("M", -10, 0.4), ("L", -14, -0.6), ("M", -10, 1), ("L", -14, 1.6)]), 0.3, 0.6)
    eyes(art, "#B8D94A", [head(-7, -3.2)], 1.8, 0.75)
    return art


# ---- Dogman -------------------------------------------------------------------------------------
# A wolf's head on a man's body, walking upright with a forward stoop, arms long and clawed.
# Amber eyeshine. Leaner than Bigfoot, with a ruff of fur at the neck and pointed ears.

DOG_FAR = "#1E1B18"


def dogman(phase):
    thigh, shin = 22.0, 21.0
    near_leg = biped_leg((0, 0), phase, thigh, shin, 26, 14, 52, lean=6)
    far_leg = biped_leg((0, 0), phase + math.pi, thigh, shin, 26, 14, 52, lean=6)
    feet = [biped_foot(l[3], l[4], 2.5, 8.0, 3.6) for l in (near_leg, far_leg)]
    drop = GROUND - lowest(feet[0] + feet[1])
    hip = (50.0, drop)

    def place(p):
        return (p[0] + hip[0], p[1] + hip[1])

    rock = 1.5 * math.sin(2 * phase)
    torso = Frame(hip, 20 + rock)
    shoulder = torso(0, -24)
    neck = torso(-5, -28)
    nod = 3 * math.sin(2 * phase + 0.8)
    head = Frame(neck, -14 + nod)

    def arm(ph):
        fwd = max(0.0, -math.sin(ph))
        upper = -2 - 26 * math.sin(ph)
        elbow = ray(shoulder, upper, 17)
        lower = upper + 10 + 24 * fwd
        wrist = ray(elbow, lower, 16)
        hand = ray(wrist, lower + 6 + 10 * fwd, 4.5)
        return elbow, wrist, hand

    near_arm = arm(phase + math.pi)
    far_arm = arm(phase)
    art = Art()

    def leg_parts(leg, foot, color):
        _, knee, _, ankle, _ = leg
        k, a = place(knee), place(ankle)
        h = place((0, 0))
        art.fill(color, capsule(h, k, 6.4, 4.2) + capsule(k, a, 4.0, 2.6) + foot_path([place(p) for p in foot]))
        return h, k, a

    def arm_parts(a, color):
        e, w, hd = a
        art.fill(color, capsule(shoulder, e, 4.6, 3.4) + capsule(e, w, 3.4, 2.6) + capsule(w, hd, 2.6, 2.0))
        d = math.degrees(math.atan2(hd[0] - w[0], -(hd[1] - w[1])))
        claws = "".join(segment(hd, ray(hd, -d + 180 + s, 3.6), 0, 1) for s in (-25, 0, 25))
        art.stroke(color, claws, 0.8)

    arm_parts(far_arm, DOG_FAR)
    leg_parts(far_leg, feet[1], DOG_FAR)

    body = shape(torso, [
        ("M", 6, 4), ("C", 9, -5, 10, -15, 8, -23), ("C", 6, -29, 1, -32, -4, -31),
        ("L", -9, -27), ("C", -11.5, -22, -10.5, -15, -8, -10), ("C", -6.5, -6, -6, -2, -5, 4), ("Z",),
    ])
    # A ruff of shaggy fur over the shoulders and down the throat.
    ruff = shape(torso, [
        ("M", 4, -27), ("L", 7.5, -22), ("L", 5, -21.5), ("L", 7, -17), ("L", 3, -19), ("L", -2, -17),
        ("L", -5, -20), ("L", -9.5, -18.5), ("L", -8.5, -23), ("L", -11, -25), ("L", -5, -31), ("Z",),
    ])
    skull = shape(head, [
        ("M", 4, -2), ("C", 4, -8, 0, -11, -4, -10.5), ("C", -7, -10, -8.5, -8, -10, -6.5),
        ("L", -18, -5), ("C", -19.5, -4.6, -19.5, -2.6, -18, -2.2), ("L", -10, -0.5),
        ("C", -7, 1.5, 0, 3, 4, -2), ("Z",),
    ])
    ears = shape(head, [("M", -2.5, -9.5), ("L", -1, -17.5), ("L", 2.5, -8.5), ("Z",),
                        ("M", 0.8, -8.5), ("L", 3.5, -15.5), ("L", 4.5, -6.5), ("Z",)])
    art.fill(INK, body + ruff + skull + ears)
    art.stroke(DETAIL, "".join([
        shape(torso, [("M", 8, -8), ("L", 10.5, -5)]), shape(torso, [("M", 9, -15), ("L", 11.5, -12)]),
        shape(torso, [("M", -7, -11), ("C", -4.5, -8, -2.5, -9, -0.5, -11)]),
        shape(head, [("M", -17.5, -3.4), ("L", -10, -2.2)]),
    ]), 0.7, 0.9)
    art.stroke("#E4DDCC", shape(head, [("M", -16, -3.2), ("L", -15.6, -1.6), ("M", -13, -2.8), ("L", -12.7, -1.4)]), 0.6, 0.85)
    art.stroke(MOON, "".join([
        shape(head, [("M", -3, -10.5), ("C", -6.5, -10, -8.5, -8, -10, -6.6), ("L", -17.5, -5.2)]),
        shape(torso, [("M", -9.5, -24), ("C", -11, -20, -10.5, -15, -8.2, -10.5)]),
        shape(head, [("M", -2.2, -10), ("L", -1.1, -16.5)]),
    ]), 0.7, 0.45)

    h, k, a = leg_parts(near_leg, feet[0], INK)
    art.stroke(DETAIL, tufts(h, k, 6.4, 3, side=-1, t0=0.2, t1=0.7), 0.7, 0.9)
    art.stroke(MOON, side_stroke(k, a, 4.0, 0.15, 0.85, 1), 0.7, 0.45)
    arm_parts(near_arm, INK)
    e, w, _ = near_arm
    art.stroke(DETAIL, tufts(shoulder, e, 4.6, 2, side=-1, t0=0.4, t1=0.85), 0.7, 0.9)
    art.stroke(MOON, side_stroke(shoulder, e, 4.6, 0.25, 0.8, 1), 0.7, 0.45)
    eyes(art, "#F0A030", [head(-8, -6.2)], 2.2, 0.85)
    return art


# ---- Hoop snake ---------------------------------------------------------------------------------
# A snake with its tail in its mouth, rolling along like a wheel. One full stride is one turn, so
# the stride is the hoop's circumference. Bands round the body and the head turning over show the roll.

HOOP_BAND = "#3A352C"
HOOP_BELLY = "#5C5444"


def hoop_snake(phase):
    art = Art()
    r = 26.0
    width = 7.0
    cx, cy = 50.0, GROUND - r - width / 2
    # Rolling left, the top of the hoop moves left: the body turns anticlockwise on screen.
    turn = phase

    def at(theta, rad=r):
        return (cx + rad * math.cos(theta), cy - rad * math.sin(theta))

    art.stroke(INK, circle((cx, cy), r), width)
    # The pale belly scales on the inside of the hoop.
    art.stroke(HOOP_BELLY, circle((cx, cy), r - width / 2 + 0.7), 1.0, 0.8)
    # Dark bands every so often round the body, turning with it.
    bands = ""
    for k in range(14):
        t = turn + k * 2 * math.pi / 14 + 0.25
        a, b = at(t, r - width / 2 + 0.9), at(t, r + width / 2 - 0.4)
        bands += f"M{f(a[0])},{f(a[1])}L{f(b[0])},{f(b[1])}"
    art.stroke(HOOP_BAND, bands, 1.6, 0.9)
    # The head bites down on the tail. It's a little wider than the body, with a stinger on the
    # tail's tip poking out past the jaw.
    h = turn + math.pi * 0.5
    head_pts = [at(h + d, r + o) for d, o in ((-0.05, -4.6), (0.08, -5.0), (0.2, -3.8), (0.27, -0.5), (0.2, 3.6), (0.08, 5.0), (-0.05, 4.6))]
    art.fill(INK, poly(head_pts))
    tip_base, tip = at(h + 0.27, r + 0.5), at(h + 0.38, r + 1.2)
    art.fill("#2A2620", poly([at(h + 0.26, r - 1.2), tip, at(h + 0.26, r + 2.0)]))
    art.stroke(DETAIL, segment(tip_base, tip, 0, 1), 0.5, 0.8)
    eye = at(h + 0.17, r + 2.2)
    eyes(art, "#E8C24A", [eye], 1.3, 0.55)
    # Moonlight along the upper left of the hoop, which stays put while the body turns under it.
    moon = "M" + "L".join(f"{f(x)},{f(y)}" for x, y in (at(math.radians(a), r + width / 2 - 0.6) for a in range(95, 200, 5)))
    art.stroke(MOON, moon, 0.8, 0.45)
    return art


# ---- Hidebehind ---------------------------------------------------------------------------------
# It's never seen in the open: it keeps a trunk between itself and whoever's looking. Here a long,
# gaunt shape leans out from behind a dead snag, one long-fingered hand on the bark, and draws back.

SNAG = "#2B2722"
SNAG_DETAIL = "#3E3830"


def hidebehind(phase):
    art = Art()
    # How far it leans out: slowly out, a pause, then quickly back behind the trunk.
    lean = 0.5 - 0.5 * math.cos(phase)
    lean = lean ** 0.7
    edge = 50.0  # the trunk's left edge
    hx = edge - 2 - 9 * lean
    head = Frame((hx, 24 + 2 * lean), -18 * lean)
    # The body behind the trunk: a narrow sliver of shoulder and side showing past the edge.
    side = poly([(edge + 2, 20), (edge - 4 - 6 * lean, 30), (edge - 3 - 4 * lean, 52), (edge - 1 - 2 * lean, 76), (edge + 2, 80)])
    skull = shape(head, [
        ("M", 3, -6), ("C", 0, -9.5, -5, -9, -6.5, -4), ("C", -7.2, -1, -6, 3, -4, 6),
        ("L", -2.5, 9), ("C", 0, 9.5, 2.5, 7, 3, 3), ("Z",),
    ])
    # One arm, very long, reaching round the trunk, its fingers curled over the bark.
    shoulder = (edge - 2 - 5 * lean, 33)
    elbow = (edge - 9 - 8 * lean, 45 - 3 * lean)
    wrist = (edge - 1, 52 - 4 * lean)
    arm = capsule(shoulder, elbow, 2.6, 1.8) + capsule(elbow, wrist, 1.8, 1.3)
    fingers = "".join(capsule(wrist, (edge + 3.5, wrist[1] - 3 + k * 2.2), 0.7, 0.5) for k in range(4))
    art.fill(INK, side + skull + arm + fingers)
    # Pale, wide-set eyes, half lidded, only once it's out far enough to look.
    if lean > 0.25:
        a = min(1.0, (lean - 0.25) / 0.4)
        art.fill("#CFC8B0", circle(head(-3.5, -2.5), 0.9), round(0.85 * a, 2))
        art.fill("#CFC8B0", circle(head(-0.2, -2.8), 0.7), round(0.5 * a, 2))
    # The snag: a broken, barkless trunk with a jagged top, drawn over the creature.
    trunk = poly([(edge, 95), (edge + 1, 60), (edge + 0.5, 30), (edge + 1.5, 10), (edge + 3, 6), (edge + 4.5, 11),
                  (edge + 6, 4), (edge + 8, 9), (edge + 10, 7), (edge + 11.5, 12), (edge + 12, 35), (edge + 13, 70),
                  (edge + 15, 95)])
    root = poly([(edge - 3, 95), (edge + 1, 86), (edge + 3, 95)]) + poly([(edge + 12, 95), (edge + 14, 85), (edge + 19, 95)])
    art.fill(SNAG, trunk + root)
    cracks = "".join(f"M{f(edge + x)},{f(y0)}L{f(edge + x + 0.6)},{f(y1)}" for x, y0, y1 in
                     ((4, 14, 40), (7.5, 22, 62), (10, 40, 88), (3, 55, 80), (6, 70, 93)))
    art.stroke(SNAG_DETAIL, cracks, 0.7, 0.9)
    art.stroke(MOON, "M{0},{1}L{2},{3}L{4},{5}".format(f(edge + 0.6), "88", f(edge + 1.1), "60", f(edge + 0.8), "30"), 0.7, 0.35)
    return art


# ---- Tommyknocker -------------------------------------------------------------------------------
# A little miner's ghost, half a man's height: big head, long beard, a pointed hood, a short pick over
# its shoulder and a lantern swinging from its forward hand.

KNOCKER_LAMP = "#F2C14E"


def tommyknocker(phase):
    thigh, shin = 9.0, 8.5
    near_leg = biped_leg((0, 0), phase, thigh, shin, 26, 10, 50, lean=2)
    far_leg = biped_leg((0, 0), phase + math.pi, thigh, shin, 26, 10, 50, lean=2)
    feet = [biped_foot(l[3], l[4], 2.0, 5.0, 2.6) for l in (near_leg, far_leg)]
    drop = GROUND - lowest(feet[0] + feet[1])
    hip = (52.0, drop)

    def place(p):
        return (p[0] + hip[0], p[1] + hip[1])

    rock = 2.5 * math.sin(2 * phase)
    torso = Frame(hip, 6 + rock)
    shoulder = torso(-1, -13)
    head = Frame(torso(-2, -17), -4 + 2 * math.sin(2 * phase + 0.8))
    art = Art()

    def leg_parts(leg, foot, color):
        _, knee, _, ankle, _ = leg
        k, a = place(knee), place(ankle)
        h = place((0, 0))
        art.fill(color, capsule(h, k, 3.6, 2.6) + capsule(k, a, 2.6, 2.0) + foot_path([place(p) for p in foot]))

    # The far arm carries a short pick over the shoulder.
    grip = torso(4, -7)
    art.fill(FAR, capsule(shoulder, grip, 2.4, 1.8))
    leg_parts(far_leg, feet[1], FAR)
    handle_top = torso(10, -24)
    art.stroke("#2C251C", segment(grip, handle_top, -0.2, 1.0), 1.3)
    art.fill(INK, shape(Frame(handle_top, 6 + rock), [("M", -7, 1.5), ("C", -3, -1.5, 3, -1.5, 7, 2.5), ("L", 6.5, 3.2), ("C", 3, 1, -3, 1, -6.8, 2.6), ("Z",)]))

    body = shape(torso, [
        ("M", 5, 2), ("C", 7, -4, 7, -10, 5, -14), ("C", 2, -17, -4, -17, -7, -14),
        ("C", -9, -10, -8.5, -4, -6, 2), ("Z",),
    ])
    hood = shape(head, [
        ("M", 6, 1), ("C", 7, -5, 6, -9, 4, -11), ("C", 8, -11, 11, -8, 12, -3),
        ("C", 9, -6, 6, -8, 1, -13.5), ("C", -4, -13, -8.5, -8, -8, -3), ("Z",),
    ])
    face = shape(head, [("M", -8.5, -6), ("C", -9.5, -4, -9.5, -2, -8, -1), ("L", -5, -1), ("L", -5, -6), ("Z",)])
    # A long beard down to the belt, stirring as it walks.
    sway = 1.2 * math.sin(2 * phase + 1.2)
    beard = shape(head, [("M", -8.5, -1.5), ("C", -10, 3, -9 + sway, 9, -6 + sway, 13), ("C", -3, 9, -1, 4, 1, -1), ("Z",)])
    art.fill(INK, body + hood + face)
    art.fill("#2A2620", beard)
    art.stroke(DETAIL, "".join(shape(head, [("M", -7 + d, 0), ("Q", -7.5 + d + sway * 0.5, 6, -6 + d + sway, 11)]) for d in (0, 2.2)), 0.5, 0.9)
    art.stroke(DETAIL, shape(torso, [("M", -7.5, -3), ("L", 5.5, -3)]), 1.0, 0.9)

    leg_parts(near_leg, feet[0], INK)
    # The near arm hangs forward holding the lantern, which swings on its bail.
    swing = 14 * math.sin(phase + math.pi)
    elbow = ray(shoulder, -18 + swing * 0.6, 6.5)
    hand = ray(elbow, -30 + swing * 0.6, 6)
    art.fill(INK, capsule(shoulder, elbow, 2.4, 1.9) + capsule(elbow, hand, 1.9, 1.6))
    lamp_angle = -swing * 0.4
    lamp = Frame(ray(hand, lamp_angle, 3.5), lamp_angle)
    art.stroke(DETAIL, segment(hand, lamp(0, 0), 0, 1), 0.5)
    art.fill(KNOCKER_LAMP, circle(lamp(0, 3.4), 6.5), 0.22)
    art.fill(INK, shape(lamp, [("M", -2.2, 0), ("L", 2.2, 0), ("L", 2.6, 1.2), ("L", -2.6, 1.2), ("Z",),
                               ("M", -2.4, 5.6), ("L", 2.4, 5.6), ("L", 2.8, 6.8), ("L", -2.8, 6.8), ("Z",)]))
    art.fill(KNOCKER_LAMP, shape(lamp, [("M", -1.8, 1.2), ("L", 1.8, 1.2), ("L", 2.1, 5.6), ("L", -2.1, 5.6), ("Z",)]))
    art.stroke(MOON, "".join([
        shape(head, [("M", -5, -11.5), ("C", -2, -13.5, 2, -13.5, 4.5, -11)]),
        shape(torso, [("M", -7, -13), ("C", -8.6, -9, -8.4, -4, -6.6, 1)]),
    ]), 0.6, 0.45)
    eyes(art, KNOCKER_LAMP, [head(-6.8, -4.2)], 1.3, 0.5)
    return art


# ---- Caiman -------------------------------------------------------------------------------------
# A small crocodilian on sprawling legs, belly nearly on the ground, a long flat snout and a heavy
# tail that swings side to side as it walks. Ridged scutes along the back, and eyes up on top of its head.

CAIMAN = "#151612"
CAIMAN_FAR = "#24251F"
CAIMAN_DETAIL = "#34362D"


def caiman(phase):
    art = Art()
    ground = GROUND
    belly = ground - 4.5
    sway = math.sin(phase)

    def leg(x, ph, color):
        # A sprawled leg: the elbow sticks out and up, the foot plants ahead and pushes back.
        reach = 5.5 * math.sin(ph)
        lift = max(0.0, -math.cos(ph)) * 2.4
        hip = (x, belly - 2.5)
        elbow = (x - 2 - reach * 0.4, belly - 5.5 - lift * 0.5)
        foot = (x - 3 - reach, ground - 1.2 - lift)
        art.fill(color, capsule(hip, elbow, 2.6, 2.0) + capsule(elbow, foot, 2.0, 1.5))
        toes = "".join(segment(foot, (foot[0] - 2.6 + k * 1.3, foot[1] + 1.1), 0, 1) for k in range(3))
        art.stroke(color, toes, 0.8)

    leg(30, phase + math.pi, CAIMAN_FAR)
    leg(58, phase, CAIMAN_FAR)
    # The tail: thick at the hips, thinning to a point, curving with the stride.
    tail = []
    for k in range(13):
        t = k / 12
        x = 62 + 34 * t
        y = belly - 3.5 + 3.5 * t + sway * 2.5 * t * t
        tail.append((x, y, 4.2 * (1 - t) + 0.5))
    top = [(x, y - r) for x, y, r in tail]
    bottom = [(x, y + r) for x, y, r in reversed(tail)]
    art.fill(CAIMAN, poly(top + bottom))
    # The body, long and flat, and the head with its long snout.
    body = path([
        ("M", 20, belly - 3), ("C", 22, belly - 9, 34, belly - 10, 46, belly - 10),
        ("C", 56, belly - 10, 63, belly - 9, 64, belly - 5), ("C", 64, belly - 1, 58, belly + 1, 46, belly + 1),
        ("C", 34, belly + 1, 24, belly + 1, 20, belly - 3), ("Z",),
    ])
    head = path([
        ("M", 22, belly - 7), ("C", 18, belly - 9, 14, belly - 8.5, 12, belly - 7.5), ("L", 3, belly - 6),
        ("C", 1, belly - 5.6, 1, belly - 3.6, 3, belly - 3.4), ("L", 14, belly - 2.6), ("C", 18, belly - 2.2, 22, belly - 2.5, 23, belly - 4),
        ("Z",),
    ])
    art.fill(CAIMAN, body + head)
    leg(30, phase, CAIMAN)
    leg(58, phase + math.pi, CAIMAN)
    # Scutes in rows along the back and tail, the jaw line and a few teeth.
    scutes = "".join(poly([(x - 1.1, belly - 9.6), (x, belly - 11.4), (x + 1.1, belly - 9.6)]) for x in range(26, 62, 3))
    scutes += "".join(poly([(x - 0.9, y - r + 0.2), (x, y - r - 1.4), (x + 0.9, y - r + 0.2)]) for x, y, r in tail[1:10:2])
    art.fill(CAIMAN, scutes)
    art.stroke(CAIMAN_DETAIL, "".join([
        path([("M", 3.5, belly - 4.4), ("L", 15, belly - 4), ("Q", 18, belly - 4.2, 20, belly - 5.5)]),
        path([("M", 30, belly - 6), ("Q", 40, belly - 4, 52, belly - 6)]),
    ]), 0.6, 0.9)
    art.stroke("#D9D2BF", "".join(segment((x, belly - 4.2), (x + 0.3, belly - 3.2), 0, 1) for x in (6, 9, 12)), 0.5, 0.8)
    art.stroke(MOON, path([("M", 3, belly - 6.2), ("L", 12, belly - 7.6), ("C", 16, belly - 8.8, 24, belly - 9.6, 34, belly - 10.2)]), 0.6, 0.45)
    # Eyes raised on top of the head, shining back.
    eyes(art, "#E3A83A", [(16, belly - 8.6)], 1.4, 0.6)
    return art


# ---- Kraken -------------------------------------------------------------------------------------
# A giant squid at the surface: the water heaves in a dark swell and long arms rise out of it,
# curling and uncurling, suckers along their inner edges. It never comes out any further than that.

KRAKEN = "#1A0F12"
KRAKEN_DETAIL = "#3C2328"
SWELL = "#141A1C"


def kraken(phase):
    art = Art()
    base_y = GROUND - 6.0

    def arm(root_x, lean, length, ph, width, color):
        # Each arm is a chain of short bones that curl more towards the tip, waving in its own time.
        pts = [(root_x, base_y)]
        angle = 180 + lean
        n = 16
        for k in range(n):
            t = k / (n - 1)
            angle += (2.5 + 9 * t * t) * math.sin(ph - t * 2.2) + lean * 0.05
            pts.append(ray(pts[-1], angle, length / n))
        shape_ = "".join(capsule(pts[k], pts[k + 1], width * (1 - k / n) + 0.4, width * (1 - (k + 1) / n) + 0.4) for k in range(n))
        art.fill(color, shape_)
        return pts

    arms = [
        (34, 18, 52, phase, 3.2, KRAKEN_DETAIL),
        (64, -20, 48, phase + 2.1, 3.0, KRAKEN_DETAIL),
        (42, 6, 72, phase + 0.9, 4.0, KRAKEN),
        (56, -8, 64, phase + 3.4, 3.8, KRAKEN),
        (49, -2, 40, phase + 4.6, 3.4, KRAKEN),
    ]
    for root_x, lean, length, ph, width, color in arms:
        pts = arm(root_x, lean, length, ph, width, color)
        if color == KRAKEN:
            # Pale suckers along one edge of the nearer arms.
            dots = ""
            for k in range(2, len(pts) - 2, 2):
                (ax, ay), (bx, by) = pts[k], pts[k + 1]
                n_ = math.hypot(bx - ax, by - ay) or 1
                off = width * (1 - k / len(pts)) * 0.7
                dots += circle((ax - (by - ay) / n_ * off, ay + (bx - ax) / n_ * off), 0.45 + 0.4 * (1 - k / len(pts)))
            art.fill("#8C6E6A", dots, 0.8)
    # The swell where the body lies just under the surface.
    heave = 1.2 * math.sin(phase)
    swell = path([("M", 12, GROUND), ("C", 22, GROUND - 6 - heave, 38, GROUND - 9 - heave, 50, GROUND - 9 - heave),
                  ("C", 62, GROUND - 9 - heave, 78, GROUND - 6 - heave, 88, GROUND), ("Z",)])
    art.fill(SWELL, swell)
    ripples = "".join(path([("M", x0, GROUND - 0.6), ("Q", x0 + w / 2, GROUND - 2 - 0.6 * math.sin(phase + x0), x0 + w, GROUND - 0.6)])
                      for x0, w in ((4, 10), (86, 10), (20, 12), (68, 12)))
    art.stroke(MOON, ripples, 0.6, 0.35)
    art.stroke(MOON, path([("M", 24, GROUND - 5.6 - heave), ("C", 34, GROUND - 8.6 - heave, 44, GROUND - 9.2 - heave, 50, GROUND - 9.2 - heave)]), 0.6, 0.4)
    # One great eye, just breaking the surface.
    eyes(art, "#C9D7D2", [(46, GROUND - 6 - heave)], 2.4, 1.1)
    return art

# ---- Menehune -----------------------------------------------------------------------------------
# Two of the small people of Hawaiian tradition, walking one behind the other at night, each with a
# stone on the shoulder for the wall they're building. Stocky and strong, shown only as silhouettes.

STONE = "#2A2A27"
STONE_DETAIL = "#3E3D38"


def menehune_one(art, phase, x, scale, color):
    thigh, shin = 8.0 * scale, 7.5 * scale
    near = biped_leg((0, 0), phase, thigh, shin, 24, 10, 46, lean=2)
    far = biped_leg((0, 0), phase + math.pi, thigh, shin, 24, 10, 46, lean=2)
    feet = [biped_foot(l[3], l[4], 1.8 * scale, 4.2 * scale, 2.4 * scale) for l in (near, far)]
    hip = (x, GROUND - lowest(feet[0] + feet[1]))

    def place(p):
        return (p[0] + hip[0], p[1] + hip[1])

    torso = Frame(hip, 4 + 2 * math.sin(2 * phase))
    shoulder = torso(-0.5 * scale, -12 * scale)
    far_color = FAR if color == INK else color
    for leg, foot, c in ((far, feet[1], far_color), (near, feet[0], color)):
        _, knee, _, ankle, _ = leg
        k, a = place(knee), place(ankle)
        art.fill(c, capsule(place((0, 0)), k, 3.4 * scale, 2.6 * scale) + capsule(k, a, 2.6 * scale, 2.0 * scale)
                 + foot_path([place(p) for p in foot]))
    body = shape(torso, [
        ("M", 5 * scale, 1 * scale), ("C", 7 * scale, -4 * scale, 7.5 * scale, -10 * scale, 5 * scale, -13.5 * scale),
        ("C", 2 * scale, -16 * scale, -4 * scale, -16 * scale, -6.5 * scale, -13 * scale),
        ("C", -8 * scale, -9 * scale, -7.5 * scale, -4 * scale, -5.5 * scale, 1 * scale), ("Z",),
    ])
    head = circle(torso(-1.2 * scale, -18.5 * scale), 3.6 * scale)
    art.fill(color, body + head)
    # Both arms up to steady the stone on the near shoulder.
    # The stone rides on the shoulder behind the head, one hand up to steady it.
    stone_c = torso(6.5 * scale, -16.5 * scale)
    hand = (stone_c[0] - 4 * scale, stone_c[1] + 1 * scale)
    elbow = (shoulder[0] - 3.5 * scale, shoulder[1] + 2.5 * scale)
    art.fill(color, capsule(shoulder, elbow, 2.1 * scale, 1.7 * scale) + capsule(elbow, hand, 1.7 * scale, 1.4 * scale))
    # The other arm swings with the stride.
    swing = 20 * math.sin(phase)
    low_elbow = ray(shoulder, swing, 5.5 * scale)
    low_hand = ray(low_elbow, swing + 12, 5 * scale)
    art.fill(color, capsule(shoulder, low_elbow, 2.0 * scale, 1.7 * scale) + capsule(low_elbow, low_hand, 1.7 * scale, 1.4 * scale))
    stone = shape(Frame(stone_c, 0), [
        ("M", -6 * scale, 1.5 * scale), ("C", -6.5 * scale, -2.5 * scale, -3 * scale, -4.5 * scale, 1 * scale, -4.2 * scale),
        ("C", 5 * scale, -4 * scale, 7 * scale, -1.5 * scale, 6 * scale, 2 * scale),
        ("C", 3 * scale, 3.2 * scale, -3 * scale, 3.2 * scale, -6 * scale, 1.5 * scale), ("Z",),
    ])
    art.fill(STONE, stone)
    art.stroke(STONE_DETAIL, "".join(circle((stone_c[0] + (k - 1) * 2.5 * scale, stone_c[1] - 0.5 * scale), 0.5 * scale) for k in range(3)), 0.5, 0.9)
    art.stroke(MOON, shape(Frame(stone_c, 0), [("M", -5.5 * scale, 0), ("C", -5.5 * scale, -2.5 * scale, -2 * scale, -4 * scale, 1 * scale, -3.8 * scale)]), 0.6, 0.5)
    art.stroke(MOON, shape(torso, [("M", -6.4 * scale, -12 * scale), ("C", -7.8 * scale, -8 * scale, -7.4 * scale, -4 * scale, -5.6 * scale, 0)]), 0.6, 0.4)


def menehune(phase):
    art = Art()
    # The one behind is a little smaller and fainter, a step out of time with the one in front.
    menehune_one(art, phase + 2.2, 68, 1.0, FAR)
    menehune_one(art, phase, 38, 1.15, INK)
    return art


CREATURES = {
    "bigfoot": (bigfoot, "walk", "Bigfoot mid-stride, arms swinging."),
    "prairie_giant": (prairie_giant, "walk", "The Prairie Giant: very tall and thin, long fingers at its sides."),
    "ringdocus": (ringdocus, "walk", "The Ringdocus: low-slung, high at the shoulders, head carried low, ringed tail."),
    "gumberoo": (gumberoo, "walk", "The Gumberoo: a hairless, coal-black bear, shiny-hided, with ember eyes."),
    "billiwhack": (billiwhack, "walk", "The Billiwhack Monster: upright goat-ape, twisted horns, grey-white hair, cloven hooves."),
    "nightcrawler": (nightcrawler, "walk", "A pair of Fresno Nightcrawlers: small, white, all legs and a head."),
    "lake_chelan": (lake_chelan, "walk", "The Lake Chelan Monster: a pale dragon on sprawling clawed legs, plated back, long tail."),
    "batsquatch": (batsquatch, "fly", "Batsquatch: a bluish-furred ape with bat wings as well as arms."),
    "jackalope": (jackalope, "walk", "A Jackalope bounding: a jackrabbit with a small rack of antlers."),
    "wimpus": (wimpus, "walk", "The Wimpus: a lead-grey salamander with three feathery horns each side of its head."),
    "chupacabra": (chupacabra, "walk", "The Chupacabra: hairless, grey and hunched, spines down its back, red eyes."),
    "red_ghost": (red_ghost, "walk", "The Red Ghost: a shaggy red camel, pacing, with a dead rider strapped to its hump."),
    "winged_monster": (winged_monster, "fly", "The Southwest's winged monster: featherless, leathery wings, a crested head and long beak."),
    "dark_watchers": (dark_watcher, "stand", "A Dark Watcher: a still figure in a wide hat and long cloak, stirred by the wind."),
    "mothman": (mothman, "fly", "Mothman with wings spread and red eyes."),
    "jersey_devil": (jersey_devil, "fly", "The Jersey Devil: leathery wings, a horse-like head and a forked tail."),
    "thunderbird": (thunderbird, "fly", "A Thunderbird with its wings spread wide."),
    "black_panther": (black_panther, "walk", "A black panther padding along: a long, low black cat with a heavy tail."),
    "dogman": (dogman, "walk", "Dogman: a wolf's head on a man's body, walking upright with a stoop."),
    "hoop_snake": (hoop_snake, "walk", "A hoop snake rolling along with its tail in its mouth."),
    "hidebehind": (hidebehind, "stand", "A Hidebehind leaning out from behind a dead snag, and drawing back."),
    "tommyknocker": (tommyknocker, "walk", "A Tommyknocker: a little bearded miner with a pick and a swinging lantern."),
    "caiman": (caiman, "walk", "A caiman: a small crocodilian on sprawling legs, with a long snout and heavy tail."),
    "kraken": (kraken, "stand", "A kraken: a giant squid's arms rising out of a dark swell, curling and uncurling."),
    "menehune": (menehune, "walk", "Two Menehune walking at night, each with a stone on the shoulder."),
}

CYCLE = {"walk": "Walk frame", "fly": "Wingbeat frame", "stand": "Standing frame"}


def main():
    preview = None
    if "--preview" in sys.argv:
        preview = sys.argv[sys.argv.index("--preview") + 1]
        os.makedirs(preview, exist_ok=True)
    for name, (rig, kind, about) in CREATURES.items():
        for i in range(FRAMES):
            art = rig(2 * math.pi * i / FRAMES)
            comment = f"{about} {CYCLE[kind]} {i + 1} of {FRAMES}, drawn facing left in a 100-unit box."
            with open(os.path.join(OUT, f"cryptid_{name}_{kind}_{i:02d}.xml"), "w") as out:
                out.write(art.xml(comment))
            if preview:
                with open(os.path.join(preview, f"{name}_{i:02d}.svg"), "w") as out:
                    out.write(art.svg())
    write_kotlin()
    print(f"Wrote {FRAMES} frames each for {', '.join(CREATURES)}, and CreatureFrames.kt")


def write_kotlin():
    """The frame lists for the app, so they always match the drawables written."""
    lines = [
        "// Generated by tools/cryptid_frames.py. Run it again after changing the rigs, rather than editing this file.",
        "package app.unconfirmed.game.ui",
        "",
        "import app.unconfirmed.game.R",
        "",
        f"/** Each animated cryptid's frames: {FRAMES} over one full stride for walkers, one wingbeat for fliers, or one loop for those that stand. */",
        "internal object CreatureFrames {",
    ]
    for name, (_, kind, _) in CREATURES.items():
        camel = name.split("_")[0] + "".join(w.title() for w in name.split("_")[1:])
        lines.append(f"    val {camel} = listOf(")
        for i in range(FRAMES):
            lines.append(f"        R.drawable.cryptid_{name}_{kind}_{i:02d},")
        lines.append("    )")
    lines.append("}")
    with open(KOTLIN, "w") as out:
        out.write("\n".join(lines) + "\n")


if __name__ == "__main__":
    main()
