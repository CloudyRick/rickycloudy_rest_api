CREATE TABLE IF NOT EXISTS `blog_posts` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    author_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES users(id)
);
CREATE TABLE IF NOT EXISTS `blog_images` (
                                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             blog_post_id BIGINT NOT NULL,
                                             image_url VARCHAR(255) NOT NULL,
    `alt` VARCHAR(255),
    `caption` VARCHAR(255),
    `credit` VARCHAR(255),
    `type` ENUM('JPEG', 'PNG', 'GIF'),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (blog_post_id) REFERENCES blog_posts(id)
    );