package com.example.preventforgettingmedicationandroidapp.domain.error

sealed class DomainException(message: String) : RuntimeException(message)

class ValidationException(message: String) : DomainException(message)

class ConflictException(message: String) : DomainException(message)

class InUseException(message: String) : DomainException(message)

class NotFoundException(message: String) : DomainException(message)
