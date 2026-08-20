package com.barbarajones.v2.quests;

/**
 * Thrown while parsing a quest JSON. Carries the file id in the message so a
 * datapack typo names the file that caused it instead of dumping a Gson trace.
 *
 * <p>Distinct from {@link QuestValidator.QuestGraphException}: this one means the
 * file could not be read at all, that one means the file read fine but describes
 * a questline a player could never actually finish.
 */
public class QuestSyntaxException extends RuntimeException {

    private final String raw;

    public QuestSyntaxException(String message) {
        super(message);
        this.raw = message;
    }

    /** The message without any prefix a caller may have re-wrapped it with. */
    public String getRawMessage() {
        return this.raw;
    }
}
