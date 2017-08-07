import models.Car;
import models.Light;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by shuai
 * on 2017/3/5.
 */
public class Test {

    // 设置一些常量方便表示
    private static final int North = 0;        // 方向
    private static final int South = 1;
    private static final int West = 2;
    private static final int East = 3;

    private static final int Red = 10;         // 交通灯颜色
    private static final int Green = 11;

    private static final int timeRange = 20;   // 模拟总时长
    private static final int carNumber = 20;  // 车辆总数
    private static final int lightGreenSeconds = 10; // 交通灯显示绿色的时长

    public static void main(String[] args) {

        // 南北向和东西向各设置一个交通灯就可以
        Light northLight = new Light(North, Green, 20);
        Light eastLight = new Light(East, Red, 20);

        // 随机生成车辆信息
        Car[] cars = new Car[carNumber];
        Random random = new Random();
        for (int i = 0; i < carNumber; i++) { // foreach不行，因为是数组初始化
            cars[i] = new Car(random.nextInt(4), random.nextInt(timeRange) + 1);
            // new Car(int direction, int showTime) 随机生成车辆朝向和出现时间，时间范围[1, timeRange]
            // 在Car构造函数内部随机生成了车辆ID，即车牌号
        }

        // cars数组按车辆出现时间排序的
        sortCarsByShowTime(cars);

        // 队列存储4个朝向的车辆信息，用队列为了保持车辆出现的先后关系
        Queue<Car> northCarsQueue = new LinkedBlockingQueue<>();
        Queue<Car> southCarsQueue = new LinkedBlockingQueue<>();
        Queue<Car> westCarsQueue = new LinkedBlockingQueue<>();
        Queue<Car> eastCarsQueue = new LinkedBlockingQueue<>();
        for (Car c : cars) {
            switch (c.getDirection()) {
                case North:
                    northCarsQueue.add(c); // 入队列
                    break;
                case South:
                    southCarsQueue.add(c);
                    break;
                case West:
                    westCarsQueue.add(c);
                    break;
                case East:
                    eastCarsQueue.add(c);
                    break;
                default:
                    break;
            }
        }

        // 集合方式遍历，元素不会被移除
        System.out.println("生成的车辆信息");
        for (Car c : northCarsQueue) System.out.println(c);
        for (Car c : southCarsQueue) System.out.println(c);
        for (Car c : westCarsQueue) System.out.println(c);
        for (Car c : eastCarsQueue) System.out.println(c);
        System.out.println();

        int lightGreen; // 绿灯持续时间，用这个变量，是因为绿灯持续时间是慢慢减少为0的
        int timeCounter = 0; // 时间计数器，与车辆实际通过路口的时间有关

        // 一条路双向车道，设置2个互斥信号量集
        int[] northMutex = new int[100];
        int[] southMutex = new int[100];
        int[] westMutex = new int[100];
        int[] eastMutex = new int[100];

        // 4个String存储不同方向的车辆通过信息
        String northPassInfo = "";
        String southPassInfo = "";
        String westPassInfo = "";
        String eastPassInfo = "";

        // 只要还有车，队列就执行
        while (!northCarsQueue.isEmpty() || !southCarsQueue.isEmpty() ||
                !westCarsQueue.isEmpty() || !eastCarsQueue.isEmpty()) {

            // 打印时间段信息 如 1----10s
            System.out.print((timeCounter + 1) + "----" + (timeCounter + lightGreenSeconds) + "s\t");

            // 调度车辆
            if (northLight.getColor() == Green) {       // 南北向车辆通过十字路口
                System.out.println("南北向绿灯亮\n");
                lightGreen = lightGreenSeconds;
                while (lightGreen-- > 0) { // 每经过一辆车花费时间为1，每花费1的时间最多通过2辆车，因为是双向的
                    timeCounter++; // timeCounter与车辆实际通过路口的时间有关
                    northPassInfo += passOneCarInfo(northCarsQueue, northMutex, timeCounter);
                    southPassInfo += passOneCarInfo(southCarsQueue, southMutex, timeCounter);
                }

                // 打印南北向车辆通过信息
                System.out.println(northPassInfo);
                System.out.println(southPassInfo);

                northPassInfo = ""; // 重置String，循环使用
                southPassInfo = "";

                northLight.setColor(Red); // 交通灯颜色转换
                eastLight.setColor(Green);
            } else {                                    // 东西向车辆通过十字路口
                System.out.println("东西向绿灯亮\n");
                lightGreen = lightGreenSeconds;
                while (lightGreen-- > 0) { // 每经过一辆车花费时间为1
                    timeCounter++;
                    westPassInfo += passOneCarInfo(westCarsQueue, westMutex, timeCounter);
                    eastPassInfo += passOneCarInfo(eastCarsQueue, eastMutex, timeCounter);
                }

                // 打印东西向车辆通过信息
                System.out.println(westPassInfo);
                System.out.println(eastPassInfo);

                westPassInfo = ""; // 重置String，循环使用
                eastPassInfo = "";

                eastLight.setColor(Red); // 交通灯颜色转换
                northLight.setColor(Green);
            }
        }
        // 执行完成
    }

    // String存储一辆车的经过信息，便于后面结果的显示
    private static String passOneCarInfo(Queue<Car> carsQueue, int[] roadMutex, int timeCounter) {
        String passInfo = "";
        if (!carsQueue.isEmpty()) {
            Car car = carsQueue.peek();
            if (car.getShowTime() <= timeCounter) { // 假定提前到了，在这一次绿灯亮一定可以穿行
                passInfo = car.toString() + "----" + getActualPassTime(roadMutex, car.getShowTime(), timeCounter) + "\n";
                carsQueue.remove(); // 满足条件，放行一辆车
            }
        }
        return passInfo; // 车辆出现信息 + 车辆经过时间
    }

    private static int getActualPassTime(int[] roadMutex, int showTime, int timeCounter) {
        // timeCounter-1 确保timeLower落在正确范围内，取商运算
        int timeLower = (timeCounter - 1) / lightGreenSeconds * lightGreenSeconds + 1; // 时间下界

        // 汽车出现时间 < timeLower 重置出现时间，说明汽车等待到下一个绿灯
        if (showTime <= timeLower) showTime = timeLower;

        if (roadMutex[showTime] == 0) { // 该时刻的道路资源未被占用，可通过，直接返回showTime，并将roadMutex[showTime]置1
            roadMutex[showTime] = 1;
            return showTime;
        } else {                        // 这一时刻的道路资源已被占用了，不可通过
            int sum = 0;
            for (int i = showTime; i <= timeCounter; i++) { // 查询roadMutex数组，看自己排在队列的第几位
                if (roadMutex[i] == 1) { // =1 表示showTime之后的时刻的道路资源被占用
                    sum++;
                }
            }
            roadMutex[showTime + sum] = 1; // 表示该车占用该一时刻的道路资源
            return showTime + sum; // 返回实际通过时间
        }
    }

    private static void sortCarsByShowTime(Car cars[]) {
        // copy车辆信息
        Car[] tmpCars = new Car[carNumber];
        System.arraycopy(cars, 0, tmpCars, 0, cars.length);

        // 对车辆信息按照出现时间排序
        int index = 0; // 遍历cars数组
        for (int i = 1; i <= timeRange; i++) { // showTime递增
            for (Car c : tmpCars) { // 遍历tmpCars
                if (c.getShowTime() == i) { // 找到showTime=i的车辆
                    cars[index] = c;
                    index++;
                }
            }
        }
    }
}