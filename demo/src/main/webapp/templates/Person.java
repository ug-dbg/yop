import java.time.*;
import java.util.*;

import org.yop.orm.evaluation.*;
import org.yop.orm.model.Yopable;
import org.yop.orm.annotations.*;
import org.yop.orm.query.*;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.annotations.*;

@Rest(
	path="person",
	summary = "Rest resource for people !",
	description = "A person in this example is a simple POJO with no relation"
)
@Table(name="person")
public class Person implements Yopable {

	@Id
	@Column(name="id")
	private Long id;

	@Column(name="first_name")
	private String firstName;

	@Column(name="last_name")
	private String lastName;

	@Column(name="birth_date")
	private LocalDate birthDate;
}