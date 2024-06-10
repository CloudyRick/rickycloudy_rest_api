ALTER TABLE `blog_images`
    ADD COLUMN `temporary_blog_id` CHAR(36);
ALTER TABLE `blog_posts`
    ADD COLUMN `temporary_blog_id` CHAR(36);