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

/**
 * CA used to handle inorganic aspects of simulation.
 *
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class InorganicCA {
    
    int width, height;

    public enum CellState {EMPTY, DIRT, WALL};
    CellState[] cells, cellsNext;
    
    public InorganicCA(int width, int height) {
        this.width = width;
        this.height = height;
        
        cells = new CellState[width*height];
        cellsNext = new CellState[width*height];
        for (int i=0; i<cells.length; i++) {
            cells[i] = CellState.EMPTY;
            cellsNext[i] = CellState.EMPTY;
        }
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
            cellsNext[i*width + j] = newState;
    }
    
    void acceptNewStates() {
        System.arraycopy(cellsNext, 0, cells, 0, cells.length);
    }

    CellState getNextCellState(int i, int j) {
        
        // Dirt falls down
        
        if (getCellState(i,j)==CellState.EMPTY && getCellState(i-1,j)==CellState.DIRT)
            return CellState.DIRT;
        
        if (getCellState(i,j)==CellState.DIRT && getCellState(i+1,j)==CellState.EMPTY)
            return CellState.EMPTY;
        
        return getCellState(i,j);
    }
    
    public void updateStates() {
        for (int i=0; i<height; i++) {
            for (int j=0; j<width; j++) {
                setCellState(i,j, getNextCellState(i, j));
            }
        }
        
        acceptNewStates();
    }
}