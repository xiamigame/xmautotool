package space.xiami.project.mc.mod.xmautotool.config;

import java.util.List;

/** User-defined tool kind (built-ins live in {@code ToolKindRegistry}). */
public record UserKind(String id, List<String> parents, int color) {

    public UserKind {
        parents = parents == null ? List.of() : List.copyOf(parents);
    }
}
