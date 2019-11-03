package net.nemerosa.ontrack.bdd.binding.steps.worlds

import net.nemerosa.ontrack.kdsl.model.Account

class AccountWithPassword(
        val account: Account,
        val password: String
)

infix fun Account.withPassword(password: String) = AccountWithPassword(this, password)
