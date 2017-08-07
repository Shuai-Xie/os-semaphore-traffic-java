package models;

/**
 * Created by shuai
 * on 2017/3/5.
 */
public class Light {
    private int direction;  // 灯的方向：东西，南北
    private int color;      // 灯的颜色：红，绿
    private int seconds;    // 交通灯持续时间

    public Light(int direction, int color, int seconds) {
        this.direction = direction;
        this.color = color;
        this.seconds = seconds;
    }

    public int getDirection() {
        return direction;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String toString() {
        return "交通灯方向：" + direction + "\t" + "状态：" + color + "\t" + "颜色持续时间：" + seconds;
    }
}
