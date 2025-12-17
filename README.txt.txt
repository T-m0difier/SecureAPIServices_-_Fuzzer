To be able to run the web app, in IntelliJ:

1) Download JDK 21 requirement (an error or pop up message at the top left side is going to show up when you open the code in IntelliJ, click it to download JDK 21 or do it manually from the website)

2) At the top side of the IntelliJ program window, in the middle, there is a green arrow facing to the right, click it to run the application "SecServSetApplication".

From there, give it a few seconds/minutes (depending on your machine) for the app to start.

You shall see a terminal opening, initializing the application. You will know that the application is running when you see as a last message on the terminal something like the following: "Started SecServSetApplication in 10.354 seconds (process running for 21.378)".

Then, you can start using the application. There are 2 ways to do this: 

1)use postman, and create the following endpoints:

Authentication Endpoints
Method	Endpoint	Description
POST	/auth/register	Register a new user
POST	/auth/login	User login
POST	/auth/logout	User logout

User Endpoints
Method	Endpoint	Description
GET	/users	Get all users (Admin only)
POST	/users	Create user (Admin only)
GET	/users/{id}	Get user by ID
PUT	/users/{id}	Update user
DELETE	/users/{id}	Delete user

Task Endpoints
Method	Endpoint	Description
GET	/tasks	Get all tasks
POST	/tasks	Create task
GET	/tasks/{id}	Get task by ID
PUT	/tasks/{id}	Update task
DELETE	/tasks/{id}	Delete task


One nice feature of postman is that it has an integrated AI, in which you can simply feed those endpoints as a prompt, tell it to create them for you automatically. You can either do that or make them manually one by one. From there, you can interact with the application by sending requests to each endpoint.

2) You can open cmd, and interact with the application using the following commands:

//register endpoint command
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "email": "test@example.com"
  }'

//login endpoint command

curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'

//logout endpoint command

curl -X POST http://localhost:8080/auth/logout

//register endpoint command

curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test",
    "description": "Task description",
    "status": "PENDING"
  }'

//register endpoint command

curl -X GET http://localhost:8080/tasks

// endpoint command
curl -X GET http://localhost:8080/tasks/{id}  

//Update task endpoint command

curl -X PUT http://localhost:8080/tasks/{id} \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Updated Task",
    "description": "Updated description",
    "status": "completed"
  }'
//Delete tasks endpoint command
curl -X DELETE http://localhost:8080/tasks/{id}

//Create user endpoint command
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "password123",
    "email": "newuser@example.com",
    "role": "user"
  }'

// list all user/ GET all users endpoint command
curl -X GET http://localhost:8080/users

//GET User by id endpoint command
curl -X GET http://localhost:8080/users/{id}

//update user endpoint command
curl -X PUT http://localhost:8080/users/{id} \
  -H "Content-Type: application/json" \
  -d '{
    "username": "updateduser",
    "email": "updated@example.com"
  }'

//delete user endpoint command

curl -X DELETE http://localhost:8080/users/1



Note: the {id} can be changed to whatever id from the ones that currently exist
Also, remove the \ from each command, have each command in 1 line.


Also, while running the app, you can visit the h2 console to see the tables that were created through your interaction with the app, at http://localhost:8080/h2-console endpoint through a browser


For that to work, the app must be running. Also, for the credentials needed for console to allow you in, go fin under the reources of the application the "application.properies". There, where it has this comment: "# H2 DB (persistent, file-based)"
are the credential;s for the h2 console.





