package net.rageevent.myriad;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.rageevent.myriad.api.format.Symbol;
import net.rageevent.myriad.api.format.TextFormatHolder;
import net.rageevent.myriad.api.player.UserRank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class Text {

    private volatile MiniMessage miniMessage = MiniMessage.builder()
            .tags(StandardTags.decorations())
            .build();
    private final System.Logger LOGGER = System.getLogger(Text.class.getSimpleName());

    // Normalized (lowercase, underscore) color names -> color, used for categorized color arguments.
    private Map<String, TextColor> colorsByName = Map.of();

    public void initialize(List<TextFormatHolder> textFormatHolders) {
        List<TextFormatHolder> holders = new ArrayList<>(textFormatHolders);
        holders.addAll(List.of(Symbol.values()));
        holders.addAll(List.of(UserRank.values()));

        miniMessage = MiniMessage.builder()
                .tags(TagResolver.resolver(StandardTags.decorations(), buildResolver(holders)))
                .build();
    }

    public Component convert(String text) {
        return miniMessage.deserialize(text).decoration(TextDecoration.ITALIC, false);
    }

    private Component errorResolver(String message) {
        return Component.text("Format Error", ThemeColor.DARK_RED).appendSpace().append(Component.text("(%s)".formatted(message), ThemeColor.DARK_GRAY));
    }

    @SuppressWarnings("PatternValidation")
    private TagResolver buildResolver(List<TextFormatHolder> textFormatHolders) {
        TagResolver.Builder resolver = TagResolver.builder();
        Map<String, TextColor> colors = new HashMap<>();

        for (Map.Entry<String, TextColor> textColorEntry : ThemeColor.COLOR_MAP.entrySet()) {
            String colorName = textColorEntry.getKey().toLowerCase();
            TextColor color = textColorEntry.getValue();

            Tag resolvedTag = Tag.styling(style -> style.color(color));

            resolver.tag(colorName, resolvedTag);
            if (colorName.contains("_"))
                resolver.tag(colorName.replace("_", "-"), resolvedTag);

            colors.put(colorName, color);

            LOGGER.log(System.Logger.Level.DEBUG, "Registered Color: $colorName!");
        }
        colorsByName = Map.copyOf(colors);

        Map<String, Map<String, TextFormatHolder>> categoryMap = new HashMap<>();

        categorizeFormats(resolver, textFormatHolders, categoryMap);
        registerCategorizedFormats(resolver, categoryMap);
        return resolver.build();
    }

    @SuppressWarnings("PatternValidation")
    private void categorizeFormats(TagResolver.Builder resolver, List<TextFormatHolder> textFormatHolders, Map<String, Map<String, TextFormatHolder>> categoryMap) {
        for (TextFormatHolder textFormatHolder : textFormatHolders) {
            String formatName = textFormatHolder.dub().toLowerCase();
            String textSymbol = textFormatHolder.symbol();
            TextColor symbolColor = textFormatHolder.color();

            if (!textFormatHolder.categorized() || !formatName.contains("_")) {
                Component symbolComponent = Component.text(textSymbol);
                if (symbolColor != null)
                    symbolComponent = symbolComponent.color(symbolColor);

                Tag resolvedTag = Tag.selfClosingInserting(symbolComponent);

                // Register under the real name, plus the dashed alias.
                resolver.tag(formatName, resolvedTag);
                if (formatName.contains("_"))
                    resolver.tag(formatName.replace("_", "-"), resolvedTag);
                continue;
            }

            int firstUnderscore = formatName.indexOf("_");

            String category = formatName.substring(0, firstUnderscore);
            String symbolName = formatName.substring(firstUnderscore + 1);

            categoryMap.computeIfAbsent(category, _ -> new HashMap<>()).put(symbolName, textFormatHolder);
        }
    }

    @SuppressWarnings("PatternValidation")
    private void registerCategorizedFormats(TagResolver.Builder resolver, Map<String, Map<String, TextFormatHolder>> categoryMap) {
        for (Map.Entry<String, Map<String, TextFormatHolder>> categoryEntry : categoryMap.entrySet()) {
            String categoryName = categoryEntry.getKey();
            Map<String, TextFormatHolder> formatChildren = categoryEntry.getValue();

            resolver.tag(categoryName, (argumentQueue, _) -> {
                if (!argumentQueue.hasNext())
                    return Tag.selfClosingInserting(errorResolver("NO ARG QUEUE"));

                String argument = normalize(argumentQueue.pop().value());
                TextFormatHolder child = formatChildren.get(argument);

                if (child == null)
                    return Tag.selfClosingInserting(errorResolver("MISSING CHILD SYMBOL"));

                TextColor enforcedColor = child.color();
                TextComponent symbolComponent = Component.text(child.symbol());

                if (enforcedColor != null) {
                    symbolComponent = symbolComponent.color(enforcedColor);
                } else if (argumentQueue.hasNext()) {
                    TextColor selectedColor = resolveColor(argumentQueue.pop().value());
                    if (selectedColor != null)
                        symbolComponent = symbolComponent.color(selectedColor);
                }

                return Tag.selfClosingInserting(symbolComponent);
            });
        }
    }

    private TextColor resolveColor(String input) {
        TextColor hex = TextColor.fromCSSHexString(input);
        return hex != null ? hex : colorsByName.get(normalize(input));
    }

    private String normalize(String input) {
        return input.toLowerCase().replace('-', '_');
    }
}
