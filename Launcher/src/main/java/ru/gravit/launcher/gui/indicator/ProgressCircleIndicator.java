package ru.gravit.launcher.gui.indicator;

import com.sun.javafx.css.converters.SizeConverter;
import javafx.beans.property.DoubleProperty;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableProperty;
import javafx.scene.control.Control;
import javafx.scene.control.ProgressIndicator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract class ProgressCircleIndicator extends ProgressIndicator {
    public ProgressCircleIndicator() {
        this.getStylesheets().add(ProgressCircleIndicator.class.getResource("/runtime/launcher/overlay/update/circleprogress.css").toExternalForm());
    }

    public final void setInnerCircleRadius(int value) {
        innerCircleRadiusProperty().set(value);
    }

    public final DoubleProperty innerCircleRadiusProperty() {
        return innerCircleRadius;
    }

    public final double getInnerCircleRadius() {
        return innerCircleRadiusProperty().get();
    }

    /**
     * radius of the inner circle
     */
    private DoubleProperty innerCircleRadius = new StyleableDoubleProperty(60) {
        @Override
        public Object getBean() {
            return ProgressCircleIndicator.this;
        }

        @Override
        public String getName() {
            return "innerCircleRadius";
        }

        @Override
        public CssMetaData<ProgressCircleIndicator, Number> getCssMetaData() {
            return StyleableProperties.INNER_CIRCLE_RADIUS;
        }
    };

    private static class StyleableProperties {
        private static final CssMetaData<ProgressCircleIndicator, Number> INNER_CIRCLE_RADIUS = new CssMetaData<ProgressCircleIndicator, Number>(
                "-fx-inner-radius", SizeConverter.getInstance(), 60) {

            @Override
            public boolean isSettable(ProgressCircleIndicator n) {
                return n.innerCircleRadiusProperty() == null || !n.innerCircleRadiusProperty().isBound();
            }

            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(ProgressCircleIndicator n) {
                return (StyleableProperty<Number>) n.innerCircleRadiusProperty();
            }
        };

        public static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Control.getClassCssMetaData());
            styleables.add(INNER_CIRCLE_RADIUS);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }
}
