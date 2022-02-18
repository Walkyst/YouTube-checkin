package youtubecheckin.db

import org.springframework.data.annotation.Id

class Account(
    @Id
    var emailPass: String,
    var refreshToken: String,
    var aas_et: String,
    var services: String,
    var tv: Boolean,
    var timestamp: Long
)