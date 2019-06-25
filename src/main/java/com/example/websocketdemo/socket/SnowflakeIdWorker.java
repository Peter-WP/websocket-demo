package com.example.websocketdemo.socket;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.*;

/**
 * From: https://github.com/twitter/snowflake An object that generates IDs. This
 * is broken into a separate class in case we ever want to support multiple
 * worker threads per process
 *
 * 以下详细说明 Snowflake ID有64bits长，由以下三部分组成： 1.
 * time—42bits,精确到ms，那就意味着其可以表示长达(2^42-1
 * )/(1000360024*365)=139.5年，另外使用者可以自己定义一个开始纪元（epoch)，
 * 然后用(当前时间-开始纪元）算出time，这表示在time这个部分在140年的时间里是不会重复的，官方文档在这里写成了41bits，应该是写错了。
 * 另外，这里用time还有一个很重要的原因，就是可以直接更具time进行排序，对于twitter这种更新频繁的应用，时间排序就显得尤为重要了。
 *
 * 2. machine id—10bits,该部分其实由datacenterId和workerId两部分组成，这两部分是在配置文件中指明的。
 *
 * datacenterId的作用(个人看法)
 * 1).方便搭建多个生成uid的service，并保证uid不重复，比如在datacenter0将机器0，1，2组成了一个生成uid的service，
 * 而datacenter1此时也需要一个生成uid的service，从本中心获取uid显然是最快最方便的，那么它可以在自己中心搭建，
 * 只要保证datacenterId唯一。如果没有datacenterId，即用10bits，那么在搭建一个新的service前必须知道目前已经在用的id，
 * 否则不能保证生成的id唯一，比如搭建的两个uid service中都有machine
 * id为100的机器，如果其server时间相同，那么产生相同id的情况不可避免。
 *
 * 2).加快server启动速度。启动一台uid server时，会去检查zk同workerId目录中其他机器的情况，
 * 如其在zk上注册的id和向它请求返回的work_id是否相同，是否处同一个datacenter下，
 * 另外还会检查该server的时间与目前已有机器的平均时间误差是否在10s范围内等，这些检查是会耗费一定时间的。
 * 将一个datacenter下的机器数限制在32台(5bits)以内，在一定程度上也保证了server的启动速度。
 *
 * workerId是实际server机器的代号，最大到32，同一个datacenter下的workerId是不能重复的。
 * 它会被注册到zookeeper上，确保workerId未被其他机器占用，并将host:port值存入，注册成功后就可以对外提供服务了。
 *
 * sequence id
 * —12bits,该id可以表示4096个数字，它是在time相同的情况下，递增该值直到为0，即一个循环结束，此时便只能等到下一个ms到来，
 * 一般情况下4096/ms的请求是不太可能出现的，所以足够使用了。
 *
 * Snowflake ID便是通过这三部分实现了UID的产生，策略也并不复杂。
 *
 * 核心代码就是毫秒级时间41位+机器ID 10位+毫秒内序列12位
 *
 */
public class SnowflakeIdWorker {
    private final static long twepoch = 1288834974657L;
    // 机器标识位数
    private final static long workerIdBits = 5L;
    // 数据中心标识位数
    private final static long datacenterIdBits = 5L;
    // 机器ID最大值
    private final static long maxWorkerId = ~(-1L << workerIdBits);
    // 数据中心ID最大值
    private final static long maxDatacenterId = ~(-1L << datacenterIdBits);
    // 毫秒内自增位
    private final static long sequenceBits = 12L;
    // 机器ID偏左移12位
    private final static long workerIdShift = sequenceBits;
    // 数据中心ID左移17位
    private final static long datacenterIdShift = sequenceBits + workerIdBits;
    // 时间毫秒左移22位
    private final static long timestampLeftShift = sequenceBits + workerIdBits
            + datacenterIdBits;

    private final static long sequenceMask = ~(-1L << sequenceBits);

    private static long lastTimestamp = -1L;

    private long sequence = 0L;
    private final long workerId;
    private final long datacenterId;

    public SnowflakeIdWorker(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            if (workerId == -1){
                this.workerId = new Random().nextInt((int)maxWorkerId);
            }else{
                throw new IllegalArgumentException(
                        "worker Id can't be greater than %d or less than 0");
            }
        }else{
            this.workerId = workerId;
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            if (datacenterId == -1){
                this.datacenterId = new Random().nextInt((int)maxDatacenterId);
            }else{
                throw new IllegalArgumentException(
                        "datacenter Id can't be greater than %d or less than 0");
            }
        }else{
            this.datacenterId = datacenterId;
        }
    }

    public synchronized long nextId() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            try {
                throw new Exception(
                        "Clock moved backwards.  Refusing to generate id for "
                                + (lastTimestamp - timestamp) + " milliseconds");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (lastTimestamp == timestamp) {
            // 当前毫秒内，则+1
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                // 当前毫秒内计数满了，则等待下一秒
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        // ID偏移组合生成最终的ID，并返回ID
        long nextId = ((timestamp - twepoch) << timestampLeftShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift) | sequence;

        return nextId;
    }

    private long tilNextMillis(final long lastTimestamp) {
        long timestamp = this.timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = this.timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

//	////////////  test  ////////////
//
	public static void main(String[] args) throws Exception {
		final Set<Long> set = new HashSet<>();

		final SnowflakeIdWorker w1 = new SnowflakeIdWorker(-1, -1);
		final SnowflakeIdWorker w2 = new SnowflakeIdWorker(-1, -1);
		final CyclicBarrier cdl = new CyclicBarrier(100);

		for (int i = 0; i < 1000; i++) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						cdl.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (BrokenBarrierException e) {
						e.printStackTrace();
					}

					// id
					Long id = w1.nextId();
					if (set.contains(id)){
						System.out.println(id + " exists");
					}
					set.add(id);
					System.out.println(id);

					// id2
					Long id2 = w2.nextId();
					if (set.contains(id2)){
						System.out.println(id2 + " exists");
					}
					set.add(id2);
					System.out.println(id2);
				}
			}).start();
		}
		try {
			TimeUnit.SECONDS.sleep(5);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
