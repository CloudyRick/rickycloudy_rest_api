CREATE DATABASE IF NOT EXISTS rickcloudy_dev;

CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY, -- Initial schema
                                     first_name VARCHAR(100) NOT NULL, -- Initial schema
    last_name VARCHAR(100) NOT NULL, -- Initial schema
    username VARCHAR(100) UNIQUE, -- Initial schema
    email VARCHAR(255) NOT NULL UNIQUE, -- V1_1_1 (modified from VARCHAR(150))
    password VARCHAR(200) NOT NULL, -- Initial schema
    status ENUM('ACTIVE', 'DISABLED', 'DELETED', 'BANNED') NOT NULL DEFAULT 'ACTIVE', -- Initial schema
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- Initial schema
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP -- Initial schema
    );

CREATE TABLE IF NOT EXISTS blog_posts (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY, -- V1_1_2
                                          title VARCHAR(100) NOT NULL, -- V1_1_2
    content TEXT NOT NULL, -- V1_1_2
    author_id BIGINT NOT NULL, -- V1_1_2
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- V1_1_2
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- V1_1_2
    status ENUM('DRAFT', 'PUBLISHED', 'ARCHIVED', 'DELETED') NOT NULL DEFAULT 'DRAFT', -- V1_1_4 (modified from V1_1_3)
    FOREIGN KEY (author_id) REFERENCES users(id) -- V1_1_2
    );

CREATE TABLE IF NOT EXISTS blog_images (
                                           id BIGINT AUTO_INCREMENT PRIMARY KEY, -- V1_1_2
                                           blog_post_id BIGINT, -- V1_1_5 (modified from blog_id in V1_1_2)
                                           image_url VARCHAR(255) NOT NULL, -- V1_1_2
    `alt` VARCHAR(255), -- V1_1_2
    `caption` VARCHAR(255), -- V1_1_2
    `credit` VARCHAR(255), -- V1_1_2
    `type` ENUM('JPEG', 'PNG', 'GIF'), -- V1_1_2
    image_key VARCHAR(255) NOT NULL, -- V1_1_9
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- V1_1_2
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- V1_1_2
    FOREIGN KEY (blog_post_id) REFERENCES blog_posts(id) -- V1_1_6 (re-added after being dropped in V1_1_5)
    );
