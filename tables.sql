-- USER

create table User (
    U_ID serial primary key,
    U_Name varchar not null,
    U_Email varchar not null,
    U_Role varchar not null,
    U_Password varchar not null,
    RegistrationDate timestamp default NOW()
)
