package com.barbarajones.v2.build.def;

/**
 * One drawing instruction in a {@link StructureDef}. Ops are applied in the
 * order they were added, and later ops overwrite earlier ones at the same
 * position - exactly like painting layers.
 *
 * <p>Almost nobody needs to implement this directly; {@link StructureDef.Builder}
 * exposes fill/box/walls/frame/line/layer/door/bed helpers that cover everything
 * the ten shipped buildings needed. Implement it when you want a generated shape
 * (a spiral stair, a procedural roof) that would be tedious as literal layers.
 */
@FunctionalInterface
public interface StructureOp {

    void apply(PlanSink sink);
}
