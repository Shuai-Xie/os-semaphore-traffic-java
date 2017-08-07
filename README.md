[操作系统课设 - 信号量机制 - 交通信号灯模拟](http://www.jianshu.com/p/60a831b66e89)

## 四、算法设计
### 4.1 创建问题情景
设置南北向和东西向的交通灯，再随机生成任意数量的汽车。

```java
// 南北向和东西向各设置一个交通灯就可以
Light northLight = new Light(North, Green, 0); // 方向，颜色，时间
Light eastLight = new Light(East, Red, 0); // 时间不用指定，因为后面有timeCounter

// 随机生成车辆信息
Car[] cars = new Car[carNumber];
Random random = new Random();
for (int i = 0; i < carNumber; i++) { // foreach不行，因为是数组初始化
    cars[i] = new Car(random.nextInt(4), random.nextInt(timeRange) + 1);
    // new Car(int direction, int showTime) 随机生成车辆朝向和出现时间，时间范围[1, timeRange]
    // 在Car构造函数内部随机生成了车辆ID，即车牌号
}
```
### 4.2. 数据预处理

将汽车按照 direction 存入到 4 个方向队列中，存储顺序按照汽车出现时间 showTime 由小到大，稳定不稳定的排序都可以，**因为同时到达路口的汽车没有时间上先后之分，而这些同时到达的汽车正是我们需要调度的对象。**

1. 汽车对象按照出现时间排序，因为出现时间是随机指定的。
```java
// cars数组按车辆出现时间排序的
sortCarsByShowTime(cars);
```
```java
private void sortCarsByShowTime(Car cars[]) {
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
```
2. 汽车按照 direction 进入 4 个方向队列。

```java
// 车辆信息进队列
Queue<Car> northCarsQueue = new LinkedBlockingQueue<>();
Queue<Car> southCarsQueue = new LinkedBlockingQueue<>();
Queue<Car> westCarsQueue = new LinkedBlockingQueue<>();
Queue<Car> eastCarsQueue = new LinkedBlockingQueue<>();
for (Car c : cars) {
    switch (c.getDirection()) {
        case North:
            northCarsQueue.add(c);
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
```

### 4.3 信号量机制
某一时刻的道路作为互斥信号量，影响体现在汽车实际通过十字路口的时间。
```
实际经过时间 >= 到达路口时间
```
互斥信号量定义
```java
// 一条路双向车道，设置2个信互斥信号量集
int[] northMutex = new int[100];
int[] southMutex = new int[100];
int[] westMutex = new int[100];
int[] eastMutex = new int[100];
```

```java
/**
 * 获取车辆实际经过十字路口的时间
 *
 * @param roadMutex   道路互斥信号量集
 * @param showTime    汽车出现时间
 * @param timeCounter 计时器
 * @return actualPassTime 车辆实际通过路口的时间
 */
private int getActualPassTime(int[] roadMutex, int showTime, int timeCounter) {
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
```

1. 通过 timeCounter 求得绿灯刚开始亮的时间 timeLower（边界值划限）；
2. 如果 showTime <= timeLower，说明汽车已经等到这一个绿灯了，即该汽车没有在其出现时间所在的绿灯时间范围内通过路口，所以更新其 showTime 为 timeLower，这种情况下会使很多不同 showTime 的车辆出现时间都设为 timeLower，不过有两个原因可以证明这也是合理的：
① 上一个交通灯没有通过的车陆陆续续（即 showTime 不同）到达路口，因为都在这个路口等，所以到下一个交通灯的时候 showTime 就一样了；
② showTime 一样不会影响其先后关系，因为车辆已经入队列了，先后关系确定了。
3. roadMutex[showTime] == 0 说明这一时段的道路未被占用，可以通过，该车辆通过的时候，设置 roadMutex[showTime] = 1，其他车辆在这一时刻不可同过，除非是反向的车辆，因为是双向车道，比如东向车在走，西向的也可以走；
4. roadMutex[showTime] != 0 说明这一时段的道路已被占用，此时查询 roadMutex 数组，看自己排在队列的第几位，然后得到实际通过时间。


### 4.4 解决问题
- 南北向为绿灯时，东西向为红灯，南北向的汽车开始调度时，东西向的汽车等待，南北向的汽车出队列；
- 东西向为绿灯时，南北向为红灯，南北向的汽车停止出队列，东西向的汽车开始调度；
- 这个过程循环进行，直到所有的车辆通过了十字路口。


```java
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
```

通过一辆车 passOneCarInfo
- 输出该车辆的信息和实际通过时间
- 该车辆出队列

```java
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
```

### 4.5 测试
设定条件
```java
private static final int timeRange = 20;   // 模拟总时长
private static final int carNumber = 20;  // 车辆总数
private static final int lightGreenSeconds = 10; // 交通灯显示绿色的时长
```
生成的车辆信息
```
车牌号           朝向    出现时间
湘U·EM7P6	North	3
赣T·5EYJA	North	13

蒙A·2KX51	South	1
黑M·LPHN8	South	7
赣L·5TSL3	South	11
闽P·EWZMH	South	11

津G·X1J69	West	2
青F·PXF6N	West	12
苏C·L6Z7A	West	12
宁A·NTC5J	West	13
藏H·6VRJT	West	15
浙W·FYWU7	West	15
皖V·2NPLA	West	17

川U·LJSYS	East	10
桂F·EJEYK	East	12
赣S·GMB8C	East	15
港J·08QVC	East	16
港O·SR58D	East	16
台E·1H5IJ	East	18
蒙Y·YV5LD	East	18
```
调度结果
```
1----10s	南北向绿灯亮

湘U·EM7P6	North	3----3

蒙A·2KX51	South	1----1
黑M·LPHN8	South	7----7

11----20s	东西向绿灯亮

津G·X1J69	West	2----11
青F·PXF6N	West	12----12
苏C·L6Z7A	West	12----13
宁A·NTC5J	West	13----14
藏H·6VRJT	West	15----15
浙W·FYWU7	West	15----16
皖V·2NPLA	West	17----17

川U·LJSYS	East	10----11
桂F·EJEYK	East	12----12
赣S·GMB8C	East	15----15
港J·08QVC	East	16----16
港O·SR58D	East	16----17
台E·1H5IJ	East	18----18
蒙Y·YV5LD	East	18----19

21----30s	南北向绿灯亮

赣T·5EYJA	North	13----21

赣L·5TSL3	South	11----21
闽P·EWZMH	South	11----22
```
