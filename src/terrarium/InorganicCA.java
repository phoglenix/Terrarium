/*
 * Copyright (C) 2014 Tim Vaughan <tgvaughan@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package terrarium;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * CA used to handle inorganic aspects of simulation.
 * 
 * This is a block CA, which uses a 3x3 cell variant of the Margolus
 * neighbourhood. 
 *
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class InorganicCA {
    
    int width, height;
    int phase;
    
    Random random;

    public enum CellState {
        EMPTY(Angle.NONE, false, new Color(0, 0, 0, 0)),
        STEAM(Angle.UP, true, new Color(200, 200, 200, 255)),
        WATER(Angle.HORIZONTAL, false, new Color(0, 100, 255, 255)),
        DIRT(Angle.DIAG_DOWN, false, new Color(139, 69, 19, 255)),
        WALL(Angle.NONE, false, new Color(0, 0, 0, 0));
        
        /** The maximum "angle" the cell type can move */
        final Angle maxAngle;
        /** Whether gravity is reversed for this type. Flips the effect of maxAngle */
        final boolean reverseGravity;
        /** Colour of this CellState. English spelling necessary. */
        final Color colour;
        private CellState(Angle maxAngle, boolean reverseGravity, Color colour) {
            this.maxAngle = maxAngle;
            this.reverseGravity = reverseGravity;
            this.colour = colour;
        }
        /**
         * @param otherState
         * @return true if this state is considered empty wrt otherState.
         */
        public boolean isEmptyFor(CellState otherState) {
            return otherState.ordinal() > this.ordinal();
        }
    }
    
    public enum Angle {
        NONE(0,0), DOWN(0,1), DIAG_DOWN(1,1), HORIZONTAL(1,0), DIAG_UP(1,-1), UP(0,-1);
        
        final int dx;
        final int dy;
        
        private Angle(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }
        
        /** Return whether the angle is "bigger" than another (ie further from down) */
        public boolean isBiggerThan(Angle other) {
            return this.ordinal() > other.ordinal();
        }
    }
    
    CellState[] cells;
    
    public InorganicCA(int width, int height) {
        this.width = width;
        this.height = height;
        
        cells = new CellState[width*height];
        for (int i=0; i<cells.length; i++)
            cells[i] = CellState.EMPTY;
        
        random = new Random();
        phase = 0;
    }
    
    public CellState getCellState(int i, int j) {
        if (i<0 || i>=height ||j<0 || j>=width)
            return CellState.WALL;
        else
            return cells[i*width + j];
    }
    
    public void setCellState(int i, int j, CellState newState) {
        if (i<0 || i>=height || j<0 || j>= width)
            throw new IllegalArgumentException("Cannot alter wall cells.");
        else
            cells[i*width + j] = newState;
    }

    protected void swapStates(int i1, int j1, int i2, int j2) {
        CellState tmp = getCellState(i1, j1);
        setCellState(i1,j1, getCellState(i2,j2));
        setCellState(i2,j2, tmp);
    }
    
    private boolean pushState(CellState state, int i1, int j1, int i2, int j2) {
        if (getCellState(i1, j1)==state && getCellState(i2, j2).isEmptyFor(state)) {
            swapStates(i1, j1, i2, j2);
            return true;
        } else
            return false;
    }
    
    /**
     * Updates the state of a single 3x3 block of cells centred on (i,j).
     * 
     * @param i
     * @param j 
     */
    public void updateBlock(int i, int j) {

        pushState(CellState.DIRT, i,j, i+1,j+1);
        pushState(CellState.DIRT, i,j+1, i+1,j);
        pushState(CellState.DIRT, i,j, i+1, j);
        pushState(CellState.DIRT, i,j+1, i+1, j+1);
        
        pushState(CellState.WATER, i,j, i+1,j+1);
        pushState(CellState.WATER, i,j+1, i+1,j);
        pushState(CellState.WATER, i,j, i+1, j);
        pushState(CellState.WATER, i,j+1, i+1, j+1);
        
        if (!pushState(CellState.WATER, i,j, i,j+1))
            pushState(CellState.WATER, i,j+1, i,j);
        if (!pushState(CellState.WATER, i+1,j+1, i+1,j))
            pushState(CellState.WATER, i+1,j, i+1,j+1);

    }
    
    public void updateStates() {

        //phase = random.nextInt(4);
        phase = (phase+1)%4;
        for (int i=phase/2; i<height; i += 2) {
            for (int j=phase%2; j<width; j += 2) {
                updateBlock(i,j);
            }
        }
    }

    @Override
    public String toString() {
        Map<CellState,Integer> histogram = new HashMap<>();
        for (CellState state : CellState.values())
            histogram.put(state, 0);
        
        for (CellState cell : cells) {
            histogram.put(cell, histogram.get(cell) + 1);
        }

        StringBuilder sb = new StringBuilder();
        for (CellState state : CellState.values()) {
            sb.append(state).append(":").append(histogram.get(state)).append(" ");
        }
        
        return sb.toString();
    }
    
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"width\": ").append(width);
        sb.append(", \"height\": ").append(height);
        sb.append(", \"states\": [");
        for (int i=0; i<cells.length; i++) {
            if (i>0)
                sb.append(",");
            sb.append(cells[i].ordinal());
        }
        sb.append("]}");
        
        return sb.toString();
    }
}
