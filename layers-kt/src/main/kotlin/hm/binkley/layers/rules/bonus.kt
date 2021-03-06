package hm.binkley.layers.rules

import hm.binkley.layers.Rule
import hm.binkley.layers.RuleValue
import java.util.Objects.hash

/** Bonus of [otherKey] plus any bonuses from this key. */
class BonusRuleValue(val otherKey: String, default: Int) :
    RuleValue<Int>("*bonus", default, bonusRule(otherKey)) {
    override fun equals(other: Any?) = this === other
            || other is BonusRuleValue
            && default == other.default
            && otherKey == other.otherKey

    override fun hashCode() = hash(default, otherKey)

    override fun toString() =
        "${this::class.simpleName}<rule: *bonus[@$otherKey=$default]>"
}

fun Int.toBonus() = this / 2 - 5

fun bonus(otherKey: String, default: Int = 0) =
    BonusRuleValue(otherKey, default)

fun bonusRule(otherKey: String): Rule<Int> = {
    val otherValue: Int = it[otherKey]
    otherValue.toBonus() + it.myValues.sum()
}
