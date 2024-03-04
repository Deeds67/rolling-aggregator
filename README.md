# Distributed event aggregator

## Application explained

This small application is a web server with two endpoints:

- `/event` - POST endpoint that accepts a plain/text body with the event data.
- `/stats` - GET endpoint that returns a plain/text body containing the rolling (count, sum, avg) for the events received in the last 60 seconds.

Whenever events are added, they are stored in a queue. The `sum` of the `x` and `y` values, as well as the count, are calculated and stored.
Stale events are removed from the queue.
When the stats endpoint is called, the averages are calculated and returned.

### Example Payload

```csv
1607341341812,0.0442672961,1282509061
1607341339812,0.0473002562,1785397642
1607341331812,0.0899538543,1852154373
1607341271812,0.0586780604,111212764
1607341261812,0.0231608745,1539565645
1607341331812,0.7796950936,1820653756
1607341291812,0.0876221437,1194727707
1607341338812,0.0302456918,1760856798
1607341311812,0.0554600769,2127711819
1607340341812,0.0360791312,1563887091
```

## How to run the web server

```bash
./gradlew run
```

## How to run the tests

```bash
./gradlew test
```

## Mock data

```bash
java -jar producer.jar -m=http -p=8080
```

Verify output with

```bash
java -jar producer.jar -m=test -p=8080
```

Manually check output

```bash
curl http://localhost:8080/stats
```

## Design decisions

### Thread-safety

- Events are stored in a `PriorityBlockingQueue`. It is a thread-safe `PriorityQueue`.
- An `AtomicReference<BigDecimal>` is used for maintaining the sum of the X values.
- An `AtomicLong` is used for maintaining the sum of the Y values.
- An `AtomicInteger` is used for maintaining the count of events.
- A `ReentrantReadWriteLock` is used to synchronize the access to the `PriorityBlockingQueue`. It assures that no race conditions occur when updating the queue.

### Floating point precision error

- `BigDecimal` is used instead of `Double` to avoid floating point precision errors.

### Performance

- The `PriorityBlockingQueue` is used as a min-heap. It assures that the events are always sorted by timestamp,
therefore allowing us to remove the events that are older than 60 seconds by removing from the head of the queue.
Insertion speed is O(log(n)). Removal speed is O(1).
- The use of BigDecimal instead of Double comes at the cost of slower arithmetic operations.
- Locking the queue for reading and writing comes at the cost of slower performance, but ensures that no race conditions occur.
- Cleaning up of stale events is done whenever new events are added.
This slows down the `event` endpoint, but makes the `stats` endpoint faster.
This should be adjusted based on the expected usage and requirements of the application.

## Tests explained

### Unit tests

- `EventTest` validates that:
  - the event object is properly deserialized
- `RollingStatsCalculatorTest` validates that:
  - events are properly added to the event queue
  - the totals are properly summed
  - old events are removed from the sum and event queue
  - the rolling stats are properly calculated
  - everything is thread safe
- `RollingStatsResultTest` validates that:
  - the result is properly serialized
  - rounding is correct
- `ApplicationTest` validates that
  - the web server endpoints return the correct status codes and responses
