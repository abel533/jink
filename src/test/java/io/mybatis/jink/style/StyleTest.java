package io.mybatis.jink.style;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StyleTest {

    @Test
    void emptyStyleDefaults() {
        Style style = Style.EMPTY;
        assertEquals(FlexDirection.ROW, style.flexDirection());
        assertEquals(JustifyContent.FLEX_START, style.justifyContent());
        assertEquals(AlignItems.STRETCH, style.alignItems());
        assertEquals(0, style.flexGrow());
        assertEquals(1, style.flexShrink());
        assertEquals(Style.AUTO, style.width());
        assertEquals(Style.AUTO, style.height());
        assertEquals(Display.FLEX, style.display());
        assertFalse(style.bold());
        assertFalse(style.italic());
        assertNull(style.color());
    }

    @Test
    void builderSetsValues() {
        Style style = Style.builder()
                .flexDirection(FlexDirection.COLUMN)
                .width(80)
                .height(24)
                .padding(2)
                .bold(true)
                .color(Color.GREEN)
                .borderStyle(BorderStyle.SINGLE)
                .build();

        assertEquals(FlexDirection.COLUMN, style.flexDirection());
        assertEquals(80, style.width());
        assertEquals(24, style.height());
        assertEquals(2, style.paddingTop());
        assertEquals(2, style.paddingRight());
        assertEquals(2, style.paddingBottom());
        assertEquals(2, style.paddingLeft());
        assertTrue(style.bold());
        assertSame(Color.GREEN, style.color());
        assertEquals(BorderStyle.SINGLE, style.borderStyle());
    }

    @Test
    void paddingShorthands() {
        Style style = Style.builder().paddingX(3).paddingY(1).build();
        assertEquals(1, style.paddingTop());
        assertEquals(3, style.paddingRight());
        assertEquals(1, style.paddingBottom());
        assertEquals(3, style.paddingLeft());
    }

    @Test
    void marginShorthands() {
        Style style = Style.builder().marginX(2).marginY(1).build();
        assertEquals(1, style.marginTop());
        assertEquals(2, style.marginRight());
        assertEquals(1, style.marginBottom());
        assertEquals(2, style.marginLeft());
    }

    @Test
    void hasBorder() {
        assertFalse(Style.EMPTY.hasBorder());
        assertTrue(Style.builder().borderStyle(BorderStyle.SINGLE).build().hasBorder());
        assertFalse(Style.builder().borderStyle(BorderStyle.NONE).build().hasBorder());
    }

    @Test
    void horizontalVerticalCalculations() {
        Style style = Style.builder()
                .paddingTop(1).paddingBottom(2).paddingLeft(3).paddingRight(4)
                .marginTop(5).marginBottom(6).marginLeft(7).marginRight(8)
                .borderStyle(BorderStyle.SINGLE)
                .build();

        assertEquals(7, style.horizontalPadding());
        assertEquals(3, style.verticalPadding());
        assertEquals(15, style.horizontalMargin());
        assertEquals(11, style.verticalMargin());
        assertEquals(2, style.horizontalBorderWidth());
        assertEquals(2, style.verticalBorderWidth());
    }

    @Test
    void toBuilder() {
        Style original = Style.builder()
                .flexDirection(FlexDirection.COLUMN)
                .width(80)
                .color(Color.RED)
                .build();

        Style modified = original.toBuilder()
                .color(Color.GREEN)
                .build();

        assertEquals(FlexDirection.COLUMN, modified.flexDirection());
        assertEquals(80, modified.width());
        assertSame(Color.GREEN, modified.color());
    }
}
