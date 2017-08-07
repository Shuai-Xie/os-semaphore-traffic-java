package models;

import java.util.Random;

/**
 * Created by shuai
 * on 2017/3/5.
 */
public class Car {
    private String id;      // 车牌号
    private int direction;  // 行驶方向
    private int showTime;   // 出现时间，到达十字路口的时间

    public Car(int direction, int showTime) {
        this.id = generateCarID(); // Car内自带的随机生成车牌号的方法，随着车辆初始化随机生成一个车牌号
        this.direction = direction;
        this.showTime = showTime;
    }

    public String getId() {
        return id;
    }

    public int getDirection() {
        return direction;
    }

    public int getShowTime() {
        return showTime;
    }

    // 重载toString方法，打印车辆信息
    public String toString() {
        String direct;
        switch (direction) {
            case 0:
                direct = "North";
                break;
            case 1:
                direct = "South";
                break;
            case 2:
                direct = "West";
                break;
            case 3:
                direct = "East";
                break;
            default:
                direct = "";
        }
        return id + "\t" + direct + "\t" + showTime;
    }

    // 车牌号的组成一般为：省份+地区代码+5位数字/字母
    private static String generateCarID() {
        char[] provinceAbbr = { // 省份简称 4+22+5+3
                '京', '津', '沪', '渝',
                '冀', '豫', '云', '辽', '黑', '湘', '皖', '鲁', '苏', '浙', '赣',
                '鄂', '甘', '晋', '陕', '吉', '闽', '贵', '粤', '青', '川', '琼',
                '宁', '新', '藏', '桂', '蒙',
                '港', '澳', '台'
        };
        String alphas = "QWERTYUIOPASDFGHJKLZXCVBNM1234567890"; // 26个字母 + 10个数字
        Random random = new Random();
        String carID = "";

        // 省份+地区代码+·  如 湘A·
        carID += provinceAbbr[random.nextInt(34)]; // 注意：分开加，因为加的是2个char
        carID += alphas.charAt(random.nextInt(26)) + "·";

        // 5位数字/字母
        for (int i = 0; i < 5; i++) {
            carID += alphas.charAt(random.nextInt(36));
        }
        return carID;
    }
}
