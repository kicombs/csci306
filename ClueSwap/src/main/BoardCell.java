package main;

public abstract class BoardCell {
	protected char cellType;
	protected int row;
	protected int col;
	
	public boolean isWalkway() {
		if( cellType == 'W' ) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isRoom() {
		if( cellType == 'W' ) {
			return false;
		} else {
			return true;
		}
	}
	
	public abstract boolean isDoorway();
	
	public void draw() {}
	
	/*
	 * Getters
	 */
	public char getCellType() {
		return cellType;
	}
	public int getRow() {
		return row;
	}
	public int getCol() {
		return col;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BoardCell other = (BoardCell) obj;
		if (cellType != other.cellType)
			return false;
		if (col != other.col)
			return false;
		if (row != other.row)
			return false;
		return true;
	}


}
