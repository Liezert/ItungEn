package com.liezert.itungen.domain.model

enum class WalletType { MAIN, GOAL }

enum class GoalStatus { ACTIVE, ACHIEVED, CANCELLED }

enum class CategoryType { EXPENSE, INCOME }

/** Arah uang ditentukan oleh tipe; `amount` selalu positif (§3.2). */
enum class TransactionType { INCOME, EXPENSE, TRANSFER_OUT, TRANSFER_IN }
