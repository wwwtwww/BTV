package dev.frost819.newbv.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.frost819.newbv.data.db.entity.UserEntity

/**
 * 账户数据访问对象。
 *
 * 提供多账户的增删改查，所有方法为 `suspend`，需在协程中调用。
 */
@Dao
interface UserDao {
    /** 查询全部已登录账户。 */
    @Query("SELECT * FROM user")
    suspend fun getAll(): List<UserEntity>

    /**
     * 按 B 站 UID 查找账户。
     *
     * @param uid 用户 UID。
     * @return 命中的账户，无匹配返回 `null`。
     */
    @Query("SELECT * FROM user WHERE uid = :uid LIMIT 1")
    suspend fun findUserByUid(uid: Long): UserEntity?

    /** 插入一个或多个账户。 */
    @Insert
    suspend fun insert(vararg user: UserEntity)

    /**
     * 插入或替换账户。
     *
     * 当 [uid] 已存在时替换整条记录（唯一索引冲突策略）。
     *
     * 警告：REPLACE 语义是「删除旧行 + 插入新行」，会连带覆盖 `lock` 字段。
     * 登录态刷新等只关心远端字段的场景禁止使用本方法，应改用 [upsertRemoteFields]，
     * 否则并发下传入的 `lock=""` 会把用户锁静默清空。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    /**
     * 原子写入账户的远端字段（用户名、头像、凭证），并在冲突时保留本地 `lock`。
     *
     * 借 `uid` 上的唯一索引做 `INSERT ... ON CONFLICT(uid) DO UPDATE`：
     * - 首次登录：新行以 `lock=''` 默认值插入；
     * - 已存在（含并发 find→insert 竞态窗口）：仅更新 username/avatar/auth，
     *   绝不触碰 `lock`，从根本上消除 REPLACE 清空用户锁的风险。
     *
     * 依赖 SQLite 3.24+ 的 UPSERT 语法，Room 运行时自带的 SQLite 版本满足要求。
     *
     * @param uid 用户 UID（唯一索引列）。
     * @param username 用户名。
     * @param avatar 头像 URL。
     * @param auth 序列化的凭证数据。
     */
    @Query(
        """
        INSERT INTO user(uid, username, avatar, auth, `lock`)
        VALUES(:uid, :username, :avatar, :auth, '')
        ON CONFLICT(uid) DO UPDATE SET
            username = excluded.username,
            avatar = excluded.avatar,
            auth = excluded.auth
        """,
    )
    suspend fun upsertRemoteFields(
        uid: Long,
        username: String,
        avatar: String,
        auth: String,
    )

    /** 删除一个或多个账户。 */
    @Delete
    suspend fun delete(vararg user: UserEntity)

    /** 更新账户信息（如刷新用户名、头像、凭证）。 */
    @Update
    suspend fun update(user: UserEntity)
}
