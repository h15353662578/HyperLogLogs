import java.util.Scanner;

/**
 * @author Huasheng
 * @Date 2021/04/27/11:28
 * @Description
 */
public class HyperLogLog {

    private final RegisterSet registerSet;
    private final int log2m;    //log(m)
    private final double alphaMM;

    /**
     * rsd = 1.04/sqrt(m)
     * @Param rsd 相对标准偏差
     */
    public HyperLogLog(double rsd){
        this(log2m(rsd));
    }

    /**
     * rsd = 1.04/sqrt(m)
     * m = (1.04 / rsd)^2
     * @Param 相对标准偏差
     * @Return
     */
    private static int log2m(double rsd){
        return (int)(Math.log((1.106 / rsd) * (1.106 / rsd)) / Math.log(2));
    }

    private static double rsd(int log2m){
        return 1.106 / Math.sqrt(Math.exp(log2m * Math.log(2)));
    }

    /***
     * accuracy = 1.04/sqrt(2^log2m)
     *
     * @Param log2m
     */
    public HyperLogLog(int log2m){
        this(log2m,new RegisterSet(1 << log2m));
    }

    /**
     * @Param registerSet
     */
    public HyperLogLog(int log2m,RegisterSet registerSet){
        this.registerSet = registerSet;
        this.log2m = log2m;
        int m = 1 << this.log2m;

        alphaMM = getAlphaMM(log2m, m);
    }

    public boolean offerHashed(int hashedValue){
        //j代表第几个桶 取hashed的前log2m位即可
        //j介于0到m之间
        final int j = hashedValue >>> (Integer.SIZE - log2m);
        //r代表 除去前log2m位剩下部分的前导零 + 1
        final int r = Integer.numberOfLeadingZeros((hashedValue << this.log2m) | (1 << (this.log2m - 1)) +1) +1;
        return registerSet.updateIfGreater(j,r);
    }

    /**
     * 添加元素
     * @param o 要被添加的元素
     * @return
     */
    public boolean offer(Object o){
        final int x = MurmurHash.hash(0);
        return offerHashed(x);
    }

    public long cardinality(){
        double registerSum = 0;
        int count = registerSet.count;
        double zeros = 0.0;
        //count桶数量
        for(int j = 0;j < registerSet.count;j++){
            int val = registerSet.get(j);
            registerSum += 1.0 / (1 << val);
            if (val == 0){
                zeros++;
            }
        }

        double estimate = alphaMM * (1 / registerSum);

        //小数据量修正
        if (estimate <= (5.0 / 2.0) * count){
            return Math.round(linearCounting(count,(int)zeros));
        }else{
            return Math.round(estimate);
        }
    }

    /**
     * 计算count常数的取值
     * @param p log2m;
     * @param m m
     * @return
     */
    protected static double getAlphaMM(final int p, final int m){
        switch (p){
            case 4:
                return 0.673 * m * m;
            case 5:
                return 0.697 * m * m;
            case 6:
                return 0.709 * m * m;
            default:
                return (0.7213 / (1 + 1.079 / m)) * m * m;
        }
    }

    /**
     * @param m 桶的数量
     * @param V 桶中0数量的数量
     * @return
     */
    protected static double linearCounting(int m,int V){
        return m * Math.log(m / V);
    }

    public static void main(String[] args) {
        HyperLogLog hyperLogLog = new HyperLogLog(0.1325);
//        Scanner scanner = new Scanner(System.in);
        System.out.println("输入集合中的元素 x y z");
//        String x = scanner.nextLine();
//        String y = scanner.nextLine();
//        String z = scanner.nextLine();
        hyperLogLog.offer("hmx");
        hyperLogLog.offer("hyy");
        hyperLogLog.offer("hyz");
        //预算估值
        System.out.println(hyperLogLog.cardinality());
    }
}
