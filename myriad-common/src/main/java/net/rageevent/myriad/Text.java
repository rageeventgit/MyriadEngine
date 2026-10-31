package net.rageevent.myriad;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.rageevent.myriad.format.Symbol;
import net.rageevent.myriad.format.TextFormatHolder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class Text {

    private MiniMessage miniMessage = MiniMessage.builder().build();

    public void initialize(List<TextFormatHolder> textFormatHolders) {
        textFormatHolders.addAll(List.of(Symbol.values()));
        miniMessage = MiniMessage.builder().tags(buildResolver(textFormatHolders)).build();
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

        for (Map.Entry<String, TextColor> textColorEntry : ThemeColor.COLOR_MAP.entrySet()) {
            String colorName = textColorEntry.getKey();
            TextColor color = textColorEntry.getValue();

            Tag resolvedTag = Tag.styling(style -> style.color(color));

            resolver.tag(colorName, resolvedTag);

            if (colorName.contains("_"))
                resolver.tag(colorName.replace("_", "-"), resolvedTag);
        }
        Map<String, Map<String, TextFormatHolder>> categoryMap = new HashMap<>();

        categorizeFormats(resolver, textFormatHolders, categoryMap);
        registerCategorizedFormats(resolver, categoryMap);
        return resolver.build();
    }

    @SuppressWarnings("PatternValidation")
    private void categorizeFormats(TagResolver.Builder resolver, List<TextFormatHolder> textFormatHolders, Map<String, Map<String, TextFormatHolder>> categoryMap) {
        for (TextFormatHolder textFormatHolder : textFormatHolders) {
            String formatName = textFormatHolder.name().toLowerCase();
            String textSymbol = textFormatHolder.symbol();
            TextColor symbolColor = textFormatHolder.color();

            if (!textFormatHolder.categorized() || !formatName.contains("_")) {
                Component symbolComponent = Component.text(textSymbol);
                if (symbolColor != null)
                    symbolComponent = symbolComponent.color(symbolColor);

                Tag resolvedTag = Tag.selfClosingInserting(symbolComponent);
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

            resolver.tag(categoryName, ((argumentQueue, context) -> {
                if (!argumentQueue.hasNext())
                    return Tag.selfClosingInserting(errorResolver("NO ARG QUEUE"));

                String argument = argumentQueue.pop().value().toLowerCase();
                TextFormatHolder child = formatChildren.get(argument);

                if (child == null)
                    return Tag.selfClosingInserting(errorResolver("MISSING CHILD SYMBOL"));

                TextColor enforcedColor = child.color();
                TextComponent symbolComponent = Component.text(child.symbol());

                if (enforcedColor != null)
                    symbolComponent = symbolComponent.color(enforcedColor);

                if (enforcedColor == null && argumentQueue.hasNext()) {
                    String colorInput = argumentQueue.pop().value();
                    TextColor selectedColor = TextColor.fromCSSHexString(colorInput);
                    //Very ugly, don't like it honestly.
                    selectedColor = selectedColor == null ? ThemeColor.COLOR_MAP.get(colorInput) : selectedColor;

                    if (selectedColor != null)
                        symbolComponent = symbolComponent.color(selectedColor);
                }

                return Tag.selfClosingInserting(symbolComponent);
            }));

        }
    }
}
