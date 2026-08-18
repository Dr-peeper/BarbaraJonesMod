package com.barbarajones.housing;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** The verdict on a candidate room, plus exactly why it failed. */
public class HousingResult {

    public boolean valid;
    public BlockPos anchor;          // where the resident should live (room centre-ish)
    public int volume;
    public final List<String> problems = new ArrayList<>();

    public static HousingResult fail(String problem) {
        HousingResult r = new HousingResult();
        r.valid = false;
        r.problems.add(problem);
        return r;
    }

    public String summary() {
        if (this.valid) {
            return "Valid housing (" + this.volume + " blocks of air).";
        }
        return String.join("  ", this.problems);
    }
}
