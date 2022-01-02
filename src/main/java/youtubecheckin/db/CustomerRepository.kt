package youtubecheckin.db

import org.springframework.data.mongodb.repository.MongoRepository

interface AccountRepository : MongoRepository<Account, String> {
    fun findByEmailPass(emailPass: String): Account?
}