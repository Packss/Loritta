package net.perfectdreams.loritta

import com.mrpowergamerbr.loritta.LorittaLauncher
import com.mrpowergamerbr.loritta.network.Databases
import com.mrpowergamerbr.loritta.utils.Constants
import com.mrpowergamerbr.loritta.utils.extensions.await
import com.mrpowergamerbr.loritta.utils.lorittaShards
import com.mrpowergamerbr.loritta.utils.stripCodeMarks
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.perfectdreams.loritta.tables.Payments
import net.perfectdreams.loritta.utils.payments.PaymentReason
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.roundToInt

class TopDonatorsRank(val m: QuirkyStuff, val config: QuirkyConfig) {
	companion object {
		private val logger = KotlinLogging.logger {}
	}

	var task: Job? = null

	fun start() {
		logger.info { "Starting Top Donators Rank Task..." }

		task = GlobalScope.launch(LorittaLauncher.loritta.coroutineDispatcher) {
			while (true) {
				val guild = lorittaShards.getGuildById(Constants.PORTUGUESE_SUPPORT_GUILD_ID)

				if (guild != null) {
					val moneySumId = Payments.money.sum()
					val mostPayingUsers = transaction(Databases.loritta) {

						Payments.slice(Payments.userId, moneySumId)
								.select {
									Payments.paidAt.isNotNull() and
											(Payments.reason eq PaymentReason.DONATION)
								}

								.groupBy(Payments.userId)
								.orderBy(moneySumId, SortOrder.DESC)
								.limit(15)
								.toMutableList()
					}

					val message = StringBuilder()

					val topMoneyAsDisplayEntry = "R$ ${mostPayingUsers[0][moneySumId]!!.toDouble().roundToInt()}"

					mostPayingUsers.forEachIndexed { index, entry ->
						val userId = entry[Payments.userId]
						val user = guild.getMemberById(entry[Payments.userId])

						val rankEmoji = when (index) {
							0 -> "<:nothing:592370648031166524>\uD83E\uDD47"
							1 -> "<:nothing:592370648031166524>\uD83E\uDD48"
							2 -> "<:nothing:592370648031166524>\uD83E\uDD49"
							3 -> "<:nothing:592370648031166524><:kawaii_four:542823233448050688>"
							4 -> "<:nothing:592370648031166524><a:kawaii_five:542823247826386997>"
							5 -> "<:nothing:592370648031166524><:kawaii_six:542823279858286592>"
							6 -> "<:nothing:592370648031166524><a:kawaii_seven:542823307414601734>"
							7 -> "<:nothing:592370648031166524><:kawaii_eight:542823334652411936>"
							8 -> "<:nothing:592370648031166524><:kawaii_nine:542823384917213200>"
							9 -> "<:kawaii_one:542823112220344350><a:kawaii_zero:542823087649849414>"
							10 -> "<:kawaii_one:542823112220344350><:kawaii_one:542823112220344350>"
							11 -> "<:kawaii_one:542823112220344350><a:kawaii_two:542823168465829907>"
							12 -> "<:kawaii_one:542823112220344350><a:kawaii_three:542823194445348885>"
							13 -> "<:kawaii_one:542823112220344350><:kawaii_four:542823233448050688>"
							14 -> "<:kawaii_one:542823112220344350><a:kawaii_five:542823247826386997>"
							15 -> "<:kawaii_one:542823112220344350><:kawaii_six:542823279858286592>"
							16 -> "<:kawaii_one:542823112220344350><a:kawaii_seven:542823307414601734>"
							17 -> "<:kawaii_one:542823112220344350><:kawaii_eight:542823334652411936>"
							18 -> "<:kawaii_one:542823112220344350><:kawaii_nine:542823384917213200>"
							19 -> "<a:kawaii_two:542823168465829907><a:kawaii_zero:542823087649849414>"
							else -> RuntimeException("There is >$index entries, but we only support up to 19!")
						}
						val badgeEmoji = if (user != null) {
							if (guild.boosters.contains(user)) {
								"<:lori_boost:588421112786976791>"
							} else {
								"<:nothing:592370648031166524>"
							}
						} else {
							"<:nothing:592370648031166524>"
						}
						message.append(rankEmoji)
						message.append(badgeEmoji)
						message.append(" • ")
						val moneyDisplay = "R$ ${entry[moneySumId]!!.toDouble().roundToInt()}"
						message.append("`${moneyDisplay.padEnd(topMoneyAsDisplayEntry.length, ' ')}` - ")
						message.append("**")
						if (user != null) {
							message.append(user.asMention)
						} else {
							val globalUser = lorittaShards.getUserById(userId)
							if (globalUser != null) {
								message.append("${globalUser.name.stripCodeMarks()}#${globalUser.discriminator}")
							} else {
								message.append(userId.toString())
							}
						}
						message.append("**")
						message.append("\n")
					}

					for (channelId in config.topDonatorsRank.channels) {
						val channel = lorittaShards.getTextChannelById(channelId.toString())

						if (channel != null) {
							val loriMessage = channel.history.retrievePast(1)
									.await()
									.firstOrNull()

							if (loriMessage?.author?.id == LorittaLauncher.loritta.discordConfig.discord.clientId) {
								loriMessage.editMessage(message.toString()).await()
							}
						}
					}
				}

				delay(60_000)
			}
		}
	}
}