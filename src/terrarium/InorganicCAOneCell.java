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

import java.util.*;

/**
 * CA used to handle inorganic aspects of simulation.
 * 
 * This is a single-cell CA which allows all cells to update once per frame, in some order, even if
 * they were initially unable to update.
 * 
 * @author Glen Robertson
 */
public class InorganicCAOneCell extends InorganicCA {
    
    /** Reduces the frequency of steam/water conversions */
    int waterCycleDelay = 500;
    
    /** Temperature in degrees, affects ratio of steam<->water */
    int temperature = 30;
    
    /** Cheap "random" left/right decider */
    boolean moveLeftFirst = true;
    
    private static class Point {
        final int x;
        final int y;
        
        Point(int y, int x) {
            this.y = y;
            this.x = x;
        }

        @Override
        public int hashCode() {
            return x + 31 * y;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Point other = (Point) obj;
            if (x != other.x)
                return false;
            if (y != other.y)
                return false;
            return true;
        }
    }
    
    public InorganicCAOneCell(int width, int height) {
        super(width, height);
    }
    
    private boolean pushState(int i1, int j1, int i2, int j2) {
        if (getCellState(i2, j2).isEmptyFor(getCellState(i1, j1))) {
            swapStates(i1, j1, i2, j2);
            return true;
        } else
            return false;
    }
    
    /**
     * Updates the state of a single cell.
     * 
     * @param y
     * @param x
     * @param angle the angle at which to try to move the cell
     */
    public Point updateCell(int y, int x, Angle angle) {
        CellState cs = getCellState(y, x);
        if (angle.isBiggerThan(cs.maxAngle)) {
            return null;
        }
        int nextY = y + angle.dy;
        if (cs.reverseGravity) {
            nextY = y - angle.dy;
        }
        int dX = angle.dx;
        
        // "Randomise" left/right movement
        if (moveLeftFirst) {
            dX = -dX;
        }
        moveLeftFirst = !moveLeftFirst;
        if (pushState(y, x, nextY, x + dX)) {
            return new Point(nextY, x + dX);
        }
        if (pushState(y, x, nextY, x - dX)) {
            return new Point(nextY, x - dX);
        }
        
        return null;
    }
    
    /** Returns a list of the 8 neighbouring points (or fewer if they are off the edge) */
    public List<Point> neighbours(Point p) {
        List<Point> neighbours = new ArrayList<>();
        for (int i = Math.max(p.y - 1, 0); i < Math.min(p.y + 2, height); i++) {
            for (int j = Math.max(p.x - 1, 0); j < Math.min(p.x + 2, width); j++) {
                if (!(i == p.y && j == p.x)) {
                    neighbours.add(new Point(i, j));
                }
            }
        }
        return neighbours;
    }
    
    /** Returns whether all neighbours of Point p are CellState type cs */
    public boolean neighboursAll(Point p, CellState cs) {
        for (int i = Math.max(p.y - 1, 0); i < Math.min(p.y + 2, height); i++) {
            for (int j = Math.max(p.x - 1, 0); j < Math.min(p.x + 2, width); j++) {
                if (!(i == p.y && j == p.x) && getCellState(i, j) != cs) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /** Returns whether Point p is on the surface of some water */
    public boolean isWaterSurface(Point p) {
        boolean foundWater = false;
        boolean foundNonWater = false;
        for (int i = Math.max(p.y - 1, 0); i < Math.min(p.y + 2, height); i++) {
            for (int j = Math.max(p.x - 1, 0); j < Math.min(p.x + 2, width); j++) {
                if (i < p.y && getCellState(i, j) != CellState.WATER) {
                    foundNonWater = true;
                    break;
                } else if (i == p.y) {
                    break;
                } else if (i > p.y && getCellState(i, j) == CellState.WATER) {
                    foundWater = true;
                    break;
                }
            }
        }
        return foundWater && foundNonWater;
    }
    
    public void updateStates() {
        // Each cell can be updated only once per updateStates call
        Set<Point> updatedCells = new HashSet<>();
        for (Angle angle : Angle.values()) {
            if (angle == Angle.NONE) {
                continue;
            }
            List<Point> toCheck = new LinkedList<>();
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    if (!angle.isBiggerThan(getCellState(i, j).maxAngle)) { // speed optimisation
                        toCheck.add(new Point(i, j));
                    }
                }
            }
            while (!toCheck.isEmpty()) {
                Point p = toCheck.remove(0);
                if (!updatedCells.contains(p)) {
                    Point updated = updateCell(p.y, p.x, angle);
                    if (updated != null) {
                        updatedCells.add(updated);
                        toCheck.addAll(neighbours(p));
                    }
                }
            }
        }
        
        // Randomly change some water to steam, based on temperature
        // Generate some random jumps
        int numJumps = 10;
        int[] jumps = new int[numJumps];
        for (int i = 0; i < numJumps; i++) {
            int variation = temperature - random.nextInt(2*temperature);
            jumps[i] = Math.max(1, 100 - temperature + variation + waterCycleDelay);
        }
        int currentJump = 0;
        for (int i = 0; i < height * width; i += jumps[currentJump]) {
            int x = i % width;
            int y = i / width;
            currentJump = (currentJump + 1) % numJumps;
            if (getCellState(y, x) == CellState.WATER && isWaterSurface(new Point(y, x))) {
                setCellState(y, x, CellState.STEAM);
            }
        }
        
        // Randomly change some steam to water, based on temperature
        // Generate some random jumps
        for (int i = 0; i < numJumps; i++) {
            int variation = temperature - random.nextInt(2*temperature);
            jumps[i] = Math.max(1, temperature + variation + waterCycleDelay);
        }
        for (int i = 0; i < height * width; i += jumps[currentJump]) {
            int x = i % width;
            int y = i / width;
            currentJump = (currentJump + 1) % numJumps;
            if (getCellState(y, x) == CellState.STEAM
                    && neighboursAll(new Point(y, x), CellState.STEAM)) {
                setCellState(y, x, CellState.WATER);
            }
        }
    }
}
