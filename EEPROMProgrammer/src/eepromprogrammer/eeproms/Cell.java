package eepromprogrammer.eeproms;

import java.util.Objects;

public final class Cell {
    public final long address;
    public final byte data;

    public Cell(long address, byte data) {
        this.address = address;
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cell cell = (Cell) o;
        return address == cell.address && data == cell.data;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, data);
    }

    @Override
    public String toString() {
        return String.format("Cell{address = 0x%04x, data = 0x%02x}", address, data);
    }
}
