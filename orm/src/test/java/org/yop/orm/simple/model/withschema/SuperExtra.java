package org.yop.orm.simple.model.withschema;

import org.apache.commons.lang3.time.DateUtils;
import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.Id;
import org.yop.orm.annotations.JoinColumn;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Yopable;
import org.yop.orm.transform.DateTimeLegacyTransformer;
import org.yop.orm.transform.FallbackTransformer;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;

@Table(name = "simple_superextra", schema = "yop")
public class SuperExtra implements Yopable {

	@Id(sequence = "seq_SUPER_EXTRA")
	@Column(name = "id", transformer = FallbackTransformer.class)
	private Long id;

	@Column(name = "extra_size", transformer = FallbackTransformer.class)
	private Long size;

	@Column(name = "calendar_timestamp", transformer = DateTimeLegacyTransformer.class)
	private Calendar calendar = Calendar.getInstance();

	@Column(name ="date_timestamp", transformer = DateTimeLegacyTransformer.class)
	private Date date = new Date();

	@Column(name ="localdatetime_timestamp")
	private LocalDateTime localDateTime = LocalDateTime.now();

	@Column(name ="localdate_timestamp", transformer = FallbackTransformer.class)
	private LocalDate localDate = LocalDate.now();

	@Column(name ="localtime_timestamp", transformer = FallbackTransformer.class)
	private LocalTime localTime = LocalTime.now();

	@Column(name ="sqldate_timestamp", transformer = FallbackTransformer.class)
	private java.sql.Date sqlDate = new java.sql.Date(System.currentTimeMillis());

	@Column(name ="sqltimestamp_timestamp", transformer = FallbackTransformer.class)
	private Timestamp sqlTimestamp = new Timestamp(System.currentTimeMillis());

	@Column(name ="sqltime_timestamp", transformer = FallbackTransformer.class)
	private Time sqlTime = new Time(System.currentTimeMillis());

	@Column(name ="instant_timestamp", transformer = DateTimeLegacyTransformer.class)
	private Instant instant = Instant.now();

	@Column(name = "superextra_comment", transformer = FallbackTransformer.class)
	private String comment = "default comment";

	@Column(name = "bigDecimal", transformer = FallbackTransformer.class)
	private BigDecimal bigDecimal = new BigDecimal("1.23456789");

	@JoinColumn(remote = "id_super_extra")
	private Collection<Extra> extras = new ArrayList<>();

	public Collection<Extra> getExtras() {
		return this.extras;
	}

	public Long getSize() {
		return this.size;
	}

	public void setSize(Long size) {
		this.size = size;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SuperExtra that = (SuperExtra) o;
		return Objects.equals(this.size,          that.size)
			&& Objects.equals(this.calendar,      that.calendar)
			&& Objects.equals(this.date,          that.date)
			&& Objects.equals(this.localDateTime, that.localDateTime)
			&& Objects.equals(this.localDate,     that.localDate)
			&& Objects.equals(this.localTime,     that.localTime)
			&& Objects.equals(this.sqlDate,       that.sqlDate)
			&& Objects.equals(this.sqlTimestamp,  that.sqlTimestamp)
			&& Objects.equals(this.sqlTime,       that.sqlTime)
			&& Objects.equals(this.instant,       that.instant)
			&& Objects.equals(this.comment,       that.comment)
			&& Objects.equals(this.bigDecimal,    that.bigDecimal);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			this.size,
			this.calendar,
			this.date,
			this.localDateTime,
			this.localDate,
			this.localTime,
			this.sqlDate,
			this.sqlTimestamp,
			this.sqlTime,
			this.instant,
			this.comment,
			this.bigDecimal
		);
	}

	public boolean acceptable(SuperExtra that) {
		return that != null
			&& Objects.equals(this.size,       that.size)
			&& Objects.equals(this.comment,    that.comment)
			&& Objects.compare(this.bigDecimal, that.bigDecimal, Comparator.naturalOrder()) == 0
			&& acceptable(this.localDate,     that.localDate)
			&& acceptable(this.localDateTime, that.localDateTime, l -> l.truncatedTo(ChronoUnit.SECONDS))
			&& acceptable(this.localTime,     that.localTime,     LocalTime::toSecondOfDay)
			&& acceptable(this.sqlDate,       that.sqlDate,       java.sql.Date::toString)
			&& acceptable(this.sqlTimestamp,  that.sqlTimestamp,  t -> t.toInstant().truncatedTo(ChronoUnit.SECONDS))
			&& acceptable(this.sqlTime,       that.sqlTime,       Time::toString)
			&& acceptable(this.instant,       that.instant,       i -> i.truncatedTo(ChronoUnit.SECONDS))
			&& DateUtils.truncatedEquals(this.calendar, that.calendar, Calendar.MINUTE)
			&& DateUtils.truncatedEquals(this.date,     that.date,     Calendar.MINUTE)
		;
	}

	private static <T extends Comparable<? super T>> boolean acceptable(T a, T b) {
		return Objects.compare(a, b, Comparator.naturalOrder()) < 1;
	}

	private static <T, U extends Comparable<? super U>> boolean acceptable(T a, T b, Function<T, U> keyExtractor) {
		return Objects.compare(a, b, Comparator.comparing(keyExtractor)) < 1;
	}
}
