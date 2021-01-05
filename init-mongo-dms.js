db.createUser(
    {
        user: "plasma",
        pwd: "plasma",
        roles: [
            {
                role: "readWrite",
                db: "dms"
            }
        ]
    }
)