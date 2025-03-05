package com.example.campusbites.data

import com.example.campusbites.domain.model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object TestData {

    private val photo1 = Photo(
        id = "p1",
        image = "https://via.placeholder.com/150",
        description = "Profile Photo"
    )
    private val photo2 = Photo(
        id = "p2",
        image = "https://via.placeholder.com/200",
        description = "Overview Photo"
    )

    private val sampleIngredient = Ingredient(
        id = 1,
        name = "Tomato",
        description = "Fresh tomato",
        products = emptyList()
    )

    private val sampleDiscount = Discount(
        id = 1,
        name = "Summer Discount",
        startDate = LocalDateTime.now().minusDays(1),
        endDate = LocalDateTime.now().plusDays(10),
        description = "10% off on all products",
        percentage = 10.0,
        discountedPrice = 900,
        isAvailable = true,
        discountedProducts = emptyList()
    )


    private val sampleInstitution = Institution(
        id = "inst1",
        name = "Universidad de Ejemplo",
        description = "Institución de prueba para la aplicación",
        members = emptyList(),
        buildings = emptyList()
    )

    private val campusBuilding = CampusBuilding(
        id = 1,
        name = "Edificio Central",
        shortName = "Central",
        latitude = 40.7128,
        longitude = -74.0060,
        institution = sampleInstitution
    )


    // Creamos los usuarios (sin institución aún)
    private var sampleUserPlaceholder: User = User(
        identification = "u1",
        name = "Juan Pérez",
        role = UserRole.UNDERGRADUATE,
        phoneNumber = "1234567890",
        isPremium = false,
        publishedAlerts = emptyList(),
        reservations = emptyList(),
        badges = emptyList(),
        schedules = emptyList(),
        institution = sampleInstitution, // Se asigna la institución de prueba
        dietaryPreferences = emptyList(),
        comments = emptyList(),
        visits = emptyList(),
        suscribedRestaurants = emptyList()
    )
    private lateinit var sampleRestaurantPlaceholder: Restaurant

    val sampleComment = Comment(
        id = 1,
        datetime = "2025-03-05T12:00:00",
        message = "Excelente servicio",
        rating = 5,
        likes = 10,
        photos = listOf(photo1),
        isVisible = true,
        author = sampleUserPlaceholder,
        responses = emptyList(),
        responseTo = null,
        reports = emptyList(),
        product = null,
        restaurant = sampleRestaurantPlaceholder
    )

    val sampleReport = Report(
        id = 1,
        datetime = LocalDateTime.now(),
        message = "Inapropiado",
        isOpen = true,
        comment = sampleComment
    )

    val sampleCommentWithReport = sampleComment.copy(
        reports = listOf(sampleReport)
    )

    val sampleReservation = Reservation(
        id = "r1",
        datetime = LocalDateTime.of(2025, 3, 5, 12, 30),
        numberCommensals = 4,
        isCompleted = false,
        user = sampleUserPlaceholder,         // Se actualizará en init
        restaurant = sampleRestaurantPlaceholder  // Se actualizará en init
    )


    val sampleVisit = Visit(
        id = "v1",
        dateTime = LocalDateTime.now(),
        vendor = sampleRestaurantPlaceholder,   // Se actualizará en init
        visitor = sampleUserPlaceholder         // Se actualizará en init
    )

    lateinit var sampleFoodSchedule: FoodSchedule
    lateinit var sampleFreetime: Freetime


    lateinit var sampleRestaurant: Restaurant
    lateinit var sampleUser: User
    lateinit var sampleUser2: User
    lateinit var sampleProduct: Product

    init {
        sampleUser = sampleUserPlaceholder
        sampleUser2 = User(
            identification = "u2",
            name = "María García",
            role = UserRole.POSTGRADUATE,
            phoneNumber = "0987654321",
            isPremium = true,
            publishedAlerts = emptyList(),
            reservations = emptyList(),
            badges = emptyList(),
            schedules = emptyList(),
            institution = sampleInstitution,
            dietaryPreferences = emptyList(),
            comments = emptyList(),
            visits = emptyList(),
            suscribedRestaurants = emptyList()
        )

        val updatedInstitution = sampleInstitution.copy(
            members = listOf(sampleUser, sampleUser2),
            buildings = listOf(campusBuilding)
        )

        sampleUser = sampleUser.copy(institution = updatedInstitution)
        sampleUser2 = sampleUser2.copy(institution = updatedInstitution)

        // Creamos un restaurante de prueba
        sampleRestaurantPlaceholder = Restaurant(
            id = 1,
            name = "La Pizzería Universitaria",
            description = "Pizzas artesanales a precio estudiantil",
            rating = 5,
            latitude = 40.7128,
            longitude = -74.0060,
            routeIndications = "Cerca de la plaza central",
            openingTime = LocalDateTime.of(2025, 3, 5, 10, 0),
            closingTime = LocalDateTime.of(2025, 3, 5, 22, 0),
            openHolidays = true,
            openWeekends = true,
            estimatedWaitTime = 15,
            isActive = true,
            overviewPhoto = photo2,
            profilePhoto = photo1,
            photos = listOf(photo2, photo1),
            reservations = emptyList(),
            alerts = emptyList(),
            subscribers = listOf(sampleUser, sampleUser2),
            visits = emptyList(),
            comments = emptyList(),
            products = emptyList(),
            tags = emptyList()
        )

        val sampleRestaurantFinal = sampleRestaurantPlaceholder
        val sampleUserFinal = sampleUser

        val updatedComment = sampleCommentWithReport.copy(
            author = sampleUserFinal,
            restaurant = sampleRestaurantFinal
        )

        val updatedReservation = sampleReservation.copy(
            user = sampleUserFinal,
            restaurant = sampleRestaurantFinal
        )

        val updatedVisit = sampleVisit.copy(
            vendor = sampleRestaurantFinal,
            visitor = sampleUser2
        )



        sampleFreetime = Freetime(
            id = "ft1",
            startHour = LocalTime.of(12, 0),
            endHour = LocalTime.of(14, 0),
            schedule = FoodSchedule(
                id = "fs1",
                name = "Lunch Time",
                isActual = true,
                user = sampleUserFinal,
                freetimes = emptyList()
            ),
            foodpreferences = emptyList()
        )
        sampleFoodSchedule = FoodSchedule(
            id = "fs1",
            name = "Lunch Time",
            isActual = true,
            user = sampleUserFinal,
            freetimes = listOf(sampleFreetime)
        )

        sampleProduct = Product(
            id = "1",
            photo = photo1,
            name = "Pepperoni Pizza",
            description = "Pizza con pepperoni y queso",
            rating = 5,
            price = 12000,
            isAvailable = true,
            comments = listOf(updatedComment),
            ingredients = listOf(sampleIngredient),
            tags = emptyList(),
            discounts = listOf(sampleDiscount),
            restaurant = sampleRestaurantFinal
        )

        sampleRestaurant = sampleRestaurantFinal.copy(
            products = listOf(sampleProduct),
            comments = listOf(updatedComment),
            reservations = listOf(updatedReservation),
            alerts = emptyList(),
            visits = listOf(updatedVisit)
        )
    }


    val restaurants: List<Restaurant> = listOf(sampleRestaurant)
    val users: List<User> = listOf(sampleUser, sampleUser2)

}
