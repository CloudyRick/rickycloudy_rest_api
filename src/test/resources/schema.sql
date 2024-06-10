-- DROP DATABASE IF EXISTS rickcloudy_dev;
CREATE TABLE IF NOT EXISTS users (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    username VARCHAR(100) UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    status ENUM('ACTIVE', 'DISABLED', 'DELETED', 'BANNED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
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
                                             blog_id BIGINT NOT NULL,
                                             image_url VARCHAR(255) NOT NULL,
    `alt` VARCHAR(255),
    `caption` VARCHAR(255),
    `credit` VARCHAR(255),
    `type` ENUM('JPEG', 'PNG', 'GIF'),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id) REFERENCES blog_posts(id)
    );
/* SET GLOBAL time_zone = '00:00'; */

