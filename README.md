# anotaciones de proyecto

## problemas

- No se pudo ejecutar migraciones con flyway de forma exitosa; se ejecutaron los SQL de forma manual en la base de datos.
- No se como es la estrategia de conexion por colas para enviar y recibir por colas separadas para realizar una comunicacion entre microservicios.


## TODO

- [ ] Mejorar la covertura
- [ ] Aplicar mas test unitarios
- [ ] Manejo de excepciones

### examples

Request Postman
```aiignore
{
  "email": "{{$randomEmail}}",
  "nombre":"{{$randomFirstName}}",
  "apellido":"{{$randomLastName}}",
  "documentoIdentidad": "{{$randomPhoneNumber}}",
  "direccion":"{{$randomDirectoryPath}}",
  "telefono": "{{$randomPhoneNumber}}",
  "salarioBase":1
}
```
