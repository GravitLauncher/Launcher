package ru.gravit.launcher.gui.buttons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.*;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableProperty;
import javafx.scene.control.Control;

import com.sun.javafx.css.converters.SizeConverter;
import javafx.scene.control.ProgressIndicator;
import ru.gravit.launcher.LauncherAPI;

@LauncherAPI
abstract class ProgressCircleIndicator extends ProgressIndicator {

	@LauncherAPI
    public ProgressCircleIndicator() {
        this.getStylesheets().add(ProgressCircleIndicator.class.getResource("/runtime/launcher/overlay/update/circleprogress.css").toExternalForm());
    }

	@LauncherAPI
    public final void setInnerCircleRadius(int value) {
        innerCircleRadiusProperty().set(value);
    }

	@LauncherAPI
    public final DoubleProperty innerCircleRadiusProperty() {
        return innerCircleRadius;
    }

	@LauncherAPI
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

    @LauncherAPI
    /**
     * @return The CssMetaData associated with this class, which may include the CssMetaData of its super classes.
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }
    
    @LauncherAPI
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }
}
