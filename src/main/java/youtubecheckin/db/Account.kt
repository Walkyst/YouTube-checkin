package youtubecheckin.db

import org.springframework.data.annotation.Id

class Account(
    @Id
    var emailPass: String,
    var aas_et: String,
    var services: String,
    var timestamp: Long,
    var refreshToken: String,
    var tv: Boolean
)