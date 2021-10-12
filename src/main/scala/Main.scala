import scalikejdbc._
import java.time._
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import os._


//case class and object for product
case class Product (id: Long, name: String, price:Long, created_at:ZonedDateTime)
object Product {
  def create(id:Long, name: String, price: Long, created_at:ZonedDateTime)(implicit s: DBSession = AutoSession): Boolean = {
    sql"insert into products (id, name, price, created_at) values (${id}, ${name}, ${price}, ${created_at})"
      .execute.apply() // returns auto-incremeneted id
  }

  
  def findById(id: Long)(implicit s: DBSession = AutoSession): Option[Product] = {
    sql"select id, name, price, created_at from products where id = ${id}"
      .map { rs => Product(rs) }.single.apply()
  }

  def updatePrice(id:Long, price:Long)(implicit s: DBSession = AutoSession): Long = {
    sql"update products set price = ${price} where id = ${id}"
    .map(rs => Product(rs)).update.apply()
  }

  def apply(rs:WrappedResultSet) = new Product(rs.long("id"), rs.string("name"), rs.long("price"), rs.zonedDateTime("created_at"))
}

// case class and object for Employee table
case class Employee(id: Long, name: String, company_id:Long, created_at:ZonedDateTime)
object Employee {
  def create(id: Long, name: String, company_id: Long, created_at:ZonedDateTime)(implicit s: DBSession = AutoSession): Boolean = {
    sql"insert into employee (id, name, company_id, created_at) values (${id},${name}, ${company_id}, ${created_at})"
      .execute.apply() // returns auto-incremeneted id
  }

  def apply(rs:WrappedResultSet) = new Employee(rs.long("id"), rs.string("name"), rs.long("company_id"), rs.zonedDateTime("created_at"))
}

// case class and object for Company table
case class Company(id: Long, name: String,created_at:ZonedDateTime)
object Company {
 
  def create(id: Long, name: String, created_at:ZonedDateTime)(implicit s: DBSession = AutoSession): Boolean = {
    sql"insert into company (id, name, created_at) values (${id}, ${name},${created_at})"
      .execute.apply() // returns auto-incremeneted id
  }

  def apply(rs:WrappedResultSet) = new Company(rs.long("id"), rs.string("name"), rs.zonedDateTime("created_at"))
  def applySpecial(rs:WrappedResultSet) = new Company(rs.long("company_id"), rs.string("company_name"), rs.zonedDateTime("created_at"))
}

// case class and object for Joined table of Employee and Company
case class EmployeeWithCompany(emp: Employee, comp:Company)

object EmployeeWithCompany{
  val * = (rs: WrappedResultSet) => new EmployeeWithCompany(
    Employee.apply(rs), 
    Company.applySpecial(rs)
  )

  // join between employee and company
  def all()(implicit  s: DBSession) : List[EmployeeWithCompany] = {
    sql"select employee.*, company.name as company_name  from employee inner join company on employee.company_id = company.id".map(*).list.apply()
  }
}

object Main extends App {
  ConnectionPool.singleton("jdbc:mysql:///data?characterEncoding=utf8", "root", "murthy#1")
  implicit val session = AutoSession
  
  //table creation for products
  sql"""create table if not exists products(
    id long not null primary key,
    name varchar(64),
    price long,
    created_at timestamp not null
  )""".execute.apply()

  //table creation for employee
  sql"""create table if not exists employee(
    id long not null primary key,
    name varchar(64),
    company_id long not null,
    created_at timestamp not null
  )""".execute.apply()

  //table creation for company
  sql"""create table if not exists company(
    id long not null primary key,
    name varchar(64),
    created_at timestamp not null
  )""".execute.apply()

  //read json and parse it using circe
  val products_json = os.read(os.pwd/"data"/"products.json")
  val decodedProducts = decode[Product](products_json)

  DB localTx { implicit session => // transactional session
    Product.create(decodedProducts.right.get.id, decodedProducts.right.get.name, decodedProducts.right.get.price, decodedProducts.right.get.created_at) // within transaction
    val product = Product.findById(decodedProducts.right.get.id) // within transaction
    Product.updatePrice(decodedProducts.right.get.id, 300)
    val updatedProduct = Product.findById(decodedProducts.right.get.id)
    val updated_price = updatedProduct.map(_.price).get 
    print(updated_price)
  }

  //read json and parse it using circe
  val company_json = os.read(os.pwd/"data"/"company.json")
  val decodedCompany = decode[Company](company_json)
  val company = decodedCompany.right.get

  //read json and parse it using circe
  val employee_json = os.read(os.pwd/"data"/"employee.json")
  val decodedEmployee = decode[Employee](employee_json)
  val employee = decodedEmployee.right.get

  DB localTx { implicit session => // transactional session
    val company_id = Company.create(company.id, company.name, company.created_at)
    Employee.create(employee.id, employee.name, employee.company_id, employee.created_at)
  }

  EmployeeWithCompany.all.foreach ((e) => println(e.emp.name + "," + e.comp.name)) // prints murthy,modak
  
  // delete all the data inserted as part of this code
  DB localTx{ implicit session => // transactional session
    sql"""truncate table employee""".execute.apply()
    sql"""truncate table company""".execute.apply()
    sql"""truncate table products""".execute.apply()
  }
}
