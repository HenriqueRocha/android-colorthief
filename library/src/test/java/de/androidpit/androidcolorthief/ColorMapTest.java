package de.androidpit.androidcolorthief;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

public class ColorMapTest {

    private MMCQ.ColorMap colorMap;

    @Mock private MMCQ.VBox vbox;
    @Mock private MMCQ.VBox vbox2;
    @Mock private MMCQ.VBox vbox3;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        colorMap = new MMCQ.ColorMap();
    }

    @Test
    public void testEmptyPalette() {
        List<int[]> palette = colorMap.palette();
        assertEquals(0, palette.size());
    }

    @Test
    public void testNonEmptyPalette() {
        final int[] avg1 = {1, 2, 3};
        final int[] avg2 = {4, 5, 6};
        final int[] avg3 = {7, 8, 9};

        when(vbox.avg(false)).thenReturn(avg1);
        when(vbox2.avg(false)).thenReturn(avg2);
        when(vbox3.avg(false)).thenReturn(avg3);

        colorMap.push(vbox);
        colorMap.push(vbox2);
        colorMap.push(vbox3);

        List<int[]> palette = colorMap.palette();

        assertEquals(avg3, palette.get(0));
        assertEquals(avg2, palette.get(1));
        assertEquals(avg1, palette.get(2));
    }

}
