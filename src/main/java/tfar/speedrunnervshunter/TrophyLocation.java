package tfar.speedrunnervshunter;


import net.minecraft.core.BlockPos;

public class TrophyLocation {

    private final int x;
    private int y;
    private final int z;
    private boolean generated;

    public TrophyLocation(int x, int z) {

        this.x = x;
        this.z = z;
    }

    public void setY(int y) {
        this.y = y;
        generated = true;
    }

    public BlockPos getPos() {
        return new BlockPos(x,y,z);
    }

    public boolean isGenerated() {
        return generated;
    }
}
